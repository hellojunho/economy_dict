import { expect, test } from '@playwright/test';

function createJwt(role = 'GENERAL') {
  const payload = {
    sub: 'e2e-user',
    role,
    exp: Math.floor(Date.now() / 1000) + 60 * 60
  };
  const encoded = Buffer.from(JSON.stringify(payload)).toString('base64url');
  return `e30.${encoded}.sig`;
}

function makeLongReply(title: string) {
  return [
    `## ${title}`,
    ...Array.from({ length: 22 }, (_, index) => `- 항목 ${index + 1}: 긴 설명이 여러 줄로 보이도록 내용을 반복합니다. 테슬라 가격 브리핑과 종합 판단, 리스크, 대응 전략을 길게 적어서 스크롤이 반드시 필요하게 만듭니다.`)
  ].join('\n\n');
}

async function seedAuth(page: Parameters<typeof test>[0]['page']) {
  const token = createJwt();
  await page.addInitScript(([accessToken]) => {
    window.localStorage.setItem('accessToken', accessToken);
  }, [token]);
}

async function expectTranscriptScrollable(page: Parameters<typeof test>[0]['page'], path: string, routes: () => Promise<void>) {
  await seedAuth(page);
  await routes();
  await page.setViewportSize({ width: 667, height: 970 });
  await page.goto(path);

  const transcript = page.locator('.advisor-transcript-scroll');
  await expect(transcript).toBeVisible();

  await expect.poll(async () => transcript.evaluate((element) => ({
    scrollHeight: element.scrollHeight,
    clientHeight: element.clientHeight
  }))).toMatchObject({
    clientHeight: expect.any(Number),
    scrollHeight: expect.any(Number)
  });

  const metrics = await transcript.evaluate((element) => ({
    clientHeight: element.clientHeight,
    scrollHeight: element.scrollHeight,
    scrollTop: element.scrollTop
  }));

  expect(metrics.scrollHeight).toBeGreaterThan(metrics.clientHeight + 120);
  expect(metrics.scrollTop).toBeGreaterThan(0);

  await transcript.evaluate((element) => {
    element.scrollTop = 0;
  });

  await expect.poll(async () => transcript.evaluate((element) => element.scrollTop)).toBe(0);

  await transcript.hover();
  await page.mouse.wheel(0, 700);

  await expect.poll(async () => transcript.evaluate((element) => element.scrollTop)).toBeGreaterThan(0);
  await expect(page.getByRole('button', { name: '맨 아래로' })).toBeVisible();
}

test('AI Recommend transcript scrolls inside the panel', async ({ page }) => {
  const thread = {
    threadId: 'recommend-thread-1',
    title: 'Tesla 전략',
    symbol: 'NASDAQ:TSLA',
    riskProfile: '균형형',
    tradeStyle: '스윙',
    notes: '',
    createdAt: '2026-03-26T09:19:00.000Z',
    updatedAt: '2026-03-26T09:19:00.000Z',
    messages: [
      {
        role: 'assistant',
        content: makeLongReply('한줄 결론'),
        createdAt: '2026-03-26T09:19:00.000Z',
        sources: [{ title: 'Example', url: 'https://example.com', domain: 'example.com' }]
      }
    ]
  };

  await expectTranscriptScrollable(page, '/ai-recommend', async () => {
    await page.route('**/api/stock-advisor/threads', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([{
          threadId: thread.threadId,
          title: thread.title,
          symbol: thread.symbol,
          riskProfile: thread.riskProfile,
          tradeStyle: thread.tradeStyle,
          createdAt: thread.createdAt,
          updatedAt: thread.updatedAt,
          messageCount: thread.messages.length
        }])
      });
    });

    await page.route(`**/api/stock-advisor/threads/${thread.threadId}`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(thread)
      });
    });
  });
});

test('AI Invest transcript scrolls inside the panel', async ({ page }) => {
  const thread = {
    threadId: 'invest-thread-1',
    title: 'Tesla 투자 분석',
    stockName: 'Tesla',
    market: '해외',
    riskProfile: '균형형',
    tradeStyle: '스윙',
    notes: '',
    createdAt: '2026-03-26T09:19:00.000Z',
    updatedAt: '2026-03-26T09:19:00.000Z',
    messages: [
      {
        role: 'assistant',
        content: makeLongReply('가격 브리핑'),
        createdAt: '2026-03-26T09:19:00.000Z',
        sources: [{ title: 'Example', url: 'https://example.com', domain: 'example.com' }]
      }
    ]
  };

  await expectTranscriptScrollable(page, '/ai-invest', async () => {
    await page.route('**/api/ai-invest/threads', async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify([{
          threadId: thread.threadId,
          title: thread.title,
          stockName: thread.stockName,
          market: thread.market,
          riskProfile: thread.riskProfile,
          tradeStyle: thread.tradeStyle,
          createdAt: thread.createdAt,
          updatedAt: thread.updatedAt,
          messageCount: thread.messages.length
        }])
      });
    });

    await page.route(`**/api/ai-invest/threads/${thread.threadId}`, async (route) => {
      await route.fulfill({
        status: 200,
        contentType: 'application/json',
        body: JSON.stringify(thread)
      });
    });
  });
});
