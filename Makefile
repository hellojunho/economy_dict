ENV_FILE := runtime.env
COMPOSE := docker compose --env-file $(ENV_FILE)
COMPOSE_PROD := docker compose --env-file $(ENV_FILE) -f docker-compose.prod.yml

.PHONY: start stop start-prod stop-prod restart build rebuild logs ps clean create-admin

start:
	$(COMPOSE) up --build

stop:
	$(COMPOSE) down

start-prod:
	$(COMPOSE_PROD) up --build

stop-prod:
	$(COMPOSE_PROD) down

restart:
	$(MAKE) stop
	$(MAKE) start

build:
	$(COMPOSE) build

rebuild:
	$(COMPOSE) build --no-cache

logs:
	$(COMPOSE) logs -f

ps:
	$(COMPOSE) ps

clean:
	$(COMPOSE) down -v

create-admin:
	$(COMPOSE) up -d db backend
	@until $(COMPOSE) exec -T db pg_isready -U postgres -d economy_dict >/dev/null 2>&1; do \
		echo "waiting for db..."; \
		sleep 2; \
	done
	@until $(COMPOSE) exec -T db psql -U postgres -d economy_dict -tAc "SELECT 1 FROM information_schema.tables WHERE table_name='users'" | grep -q 1; do \
		echo "waiting for users table..."; \
		sleep 2; \
	done
	$(COMPOSE) exec -T db psql -U postgres -d economy_dict -c "CREATE EXTENSION IF NOT EXISTS pgcrypto;"
	$(COMPOSE) exec -T db psql -U postgres -d economy_dict -c "INSERT INTO users (user_id, username, password, user_email, role, status, activated_at, deactivated_at, created_at, updated_at) VALUES ('admin', 'admin', crypt('admin123!@#', gen_salt('bf')), 'admin@admin.com', 'ADMIN', 'ACTIVE', NOW(), NULL, NOW(), NOW()) ON CONFLICT (user_id) DO UPDATE SET username = EXCLUDED.username, password = crypt('admin123!@#', gen_salt('bf')), user_email = EXCLUDED.user_email, role = 'ADMIN', status = 'ACTIVE', activated_at = COALESCE(users.activated_at, NOW()), deactivated_at = NULL, updated_at = NOW();"
