import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { Api } from './api';
export const authGuard: CanActivateFn = async (_route, state) => {
  const api = inject(Api); const router = inject(Router);
  try { await api.me(); return true; }
  catch { return router.createUrlTree(['/login'], { queryParams: { returnUrl: state.url } }); }
};
