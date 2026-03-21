import { expect, test } from '@playwright/test';

test('admin sign-in redirects to admin page', async ({ page }) => {
  await page.goto('/signin');
  await page.getByLabel('User ID').fill('admin');
  await page.getByLabel('Password').fill('admin123!@#');
  await page.getByRole('button', { name: 'Sign In' }).click();

  await expect(page).toHaveURL(/\/admin$/);
  await expect(page.getByRole('heading', { name: 'Operations' })).toBeVisible();
  await expect(page.getByRole('heading', { name: 'Overview' })).toBeVisible();
});

test('general user can sign up, sign in, and logout', async ({ page }) => {
  const suffix = Date.now();
  const userId = `e2e-${suffix}@example.com`;
  const password = 'qwer1234!@#$';

  await page.goto('/signup');
  await page.getByLabel('User ID').fill(userId);
  await page.getByLabel('Username').fill(`e2e-${suffix}`);
  await page.getByLabel('Password').fill(password);
  await page.getByLabel('Email').fill(userId);
  await page.getByRole('button', { name: 'Create Account' }).click();

  await expect(page).toHaveURL(/\/signin$/);

  await page.getByLabel('User ID').fill(userId);
  await page.getByLabel('Password').fill(password);
  await page.getByRole('button', { name: 'Sign In' }).click();

  await expect(page).toHaveURL(/\/mypage$/);
  await expect(page.getByRole('heading', { name: /학습 현황/ })).toBeVisible();

  await page.getByRole('main').getByRole('button', { name: 'Logout' }).click();
  await expect(page).toHaveURL(/\/signin$/);
});
