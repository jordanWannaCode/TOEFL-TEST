import {test,expect,Page} from '@playwright/test';
async function login(page:Page){await page.goto('/login');await page.getByLabel('Email',{exact:true}).fill(process.env['E2E_EMAIL']!);await page.getByLabel('Password',{exact:true}).fill(process.env['E2E_PASSWORD']!);await page.getByRole('button',{name:'Sign in',exact:true}).click();await page.getByRole('link',{name:'Open Writing practice'}).click();}
test('Email autosaves, resumes, creates a real job and displays persisted structured test feedback',async({page})=>{
 await login(page);await page.getByRole('button',{name:'Start Request a quieter study room'}).click();
 await expect(page.getByRole('heading',{name:'Request a quieter study room'})).toBeVisible();
 const response='Dear coordinator, our study room is next to a noisy rehearsal studio. Could we use a quieter room on Thursday afternoon? If none is available, Friday morning would also work. Thank you for your help.';
 await page.getByLabel('Your response',{exact:true}).fill(response);await expect(page.getByRole('status')).toHaveText('Saved on the server');await page.reload();await expect(page.getByLabel('Your response',{exact:true})).toHaveValue(response);
 await page.getByRole('button',{name:'Submit Writing response'}).click();await expect(page.getByTestId('writing-score')).toHaveText('3 / 5');await expect(page.getByText('Simulated evaluation for automated tests only.')).toBeVisible();await expect(page.getByText('TEST_ONLY',{exact:false}).first()).toBeVisible();
 await page.reload();await expect(page.getByTestId('writing-score')).toHaveText('3 / 5');await expect(page.locator('pre').first()).toHaveText(response);await page.getByRole('link',{name:'Back to dashboard'}).click();await expect(page.getByText('Writing practice',{exact:true}).first()).toBeVisible();await page.getByRole('link',{name:'View results'}).first().click();await expect(page.getByTestId('writing-score')).toHaveText('3 / 5');
});
test('Sentence builder works with keyboard and deterministic variants',async({page})=>{
 await login(page);await page.getByRole('button',{name:'Start A study group invitation'}).click();
 for(const text of ['We can meet','in the library','after class']){await page.getByRole('button',{name:'Add '+text,exact:true}).focus();await page.keyboard.press('Enter');}
 await expect(page.getByTestId('constructed-sentence')).toHaveText('We can meet in the library after class');
 await expect(page.getByRole('status')).toHaveText('Saved on the server');await page.getByRole('button',{name:'Submit Writing response'}).click();await expect(page.getByTestId('sentence-score')).toHaveText('1 / 1');await expect(page.getByText('Both time and place adjunct orders are grammatical.',{exact:false})).toBeVisible();
});
test('Academic discussion contributions and response persist',async({page})=>{
 await login(page);await page.getByRole('button',{name:'Start Choosing a campus sustainability project'}).click();await expect(page.getByText('Maya:',{exact:true})).toBeVisible();await expect(page.getByText('Owen:',{exact:true})).toBeVisible();await page.getByLabel('Your response',{exact:true}).fill('I support a repair workshop because it extends the life of items and teaches useful skills. Although bins reach more people, preventing waste can reduce what needs to be recycled.');await expect(page.getByRole('status')).toHaveText('Saved on the server');await page.getByRole('button',{name:'Submit Writing response'}).click();await expect(page.getByTestId('writing-score')).toHaveText('3 / 5');await expect(page.getByRole('heading',{name:'contribution',exact:false})).toBeVisible();
});
test('Custom timed Writing keeps its deadline across reload',async({page})=>{
 await login(page);await page.getByLabel('Mode',{exact:true}).selectOption('SECTION_TEST');
 const created=page.waitForResponse(r=>r.url().endsWith('/api/v1/sessions')&&r.request().method()==='POST');
 await page.getByRole('button',{name:'Start Request a quieter study room'}).click();const s=await(await created).json();expect(s.expiresAt).toBeTruthy();expect(Date.parse(s.expiresAt)-Date.parse(s.startedAt)).toBe(300000);
 await page.getByLabel('Your response',{exact:true}).fill('Please reserve a quieter room for Thursday. Friday is an alternative.');await expect(page.getByRole('status')).toHaveText('Saved on the server');
 const resumed=page.waitForResponse(r=>r.url().endsWith('/api/v1/sessions/'+s.id)&&r.request().method()==='GET');await page.reload();const after=await(await resumed).json();expect(after.expiresAt).toBe(s.expiresAt);await expect(page.getByRole('timer')).toContainText('Server time remaining');await page.getByRole('button',{name:'Submit Writing response'}).click();await expect(page.getByTestId('writing-score')).toHaveText('3 / 5');
});
test('Writing autosave survives a lost acknowledgement and refresh',async({page})=>{
 await login(page);await page.getByRole('button',{name:'Start Request a quieter study room'}).click();await expect(page.getByRole('heading',{name:'Request a quieter study room'})).toBeVisible();
 let lost=false;await page.route('**/api/v1/sessions/*/items/*/response',async route=>{if(!lost){lost=true;await route.fetch();await route.abort('failed');}else await route.continue();});
 await page.getByLabel('Your response',{exact:true}).fill('Could we please reserve a quiet room? Thursday afternoon is best.');await expect(page.getByRole('button',{name:'Retry saving',exact:true})).toBeVisible();await page.reload();await expect(page.getByRole('status')).toHaveText('Saved on the server');await expect(page.getByLabel('Your response',{exact:true})).toHaveValue('Could we please reserve a quiet room? Thursday afternoon is best.');
});
