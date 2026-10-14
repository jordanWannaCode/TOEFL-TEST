import { provideZonelessChangeDetection } from '@angular/core';
import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { AppComponent } from './app/app.component';
import { authGuard } from './app/core/auth.guard';
bootstrapApplication(AppComponent, {
  providers: [provideZonelessChangeDetection(), provideHttpClient(), provideRouter([
    { path:'login', loadComponent:() => import('./app/features/auth/login.component').then(m=>m.LoginComponent) },
    { path:'', canActivate:[authGuard], loadComponent:() => import('./app/features/dashboard/dashboard.component').then(m=>m.DashboardComponent) },
    { path:'practice/:id', canActivate:[authGuard], loadComponent:() => import('./app/features/reading/session.component').then(m=>m.SessionComponent) },
    { path:'results/:id', canActivate:[authGuard], loadComponent:() => import('./app/features/reading/results.component').then(m=>m.ResultsComponent) },
    { path:'**', redirectTo:'' },
  ])]
}).catch(error => console.error(error));
