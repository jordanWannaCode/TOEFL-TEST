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
    { path:'writing', canActivate:[authGuard], loadComponent:()=>import('./app/features/writing/catalog.component').then(m=>m.WritingCatalogComponent) },
    { path:'writing/session/:id', canActivate:[authGuard], loadComponent:()=>import('./app/features/writing/session.component').then(m=>m.WritingSessionComponent) },
    { path:'writing/results/:id', canActivate:[authGuard], loadComponent:()=>import('./app/features/writing/report.component').then(m=>m.WritingReportComponent) },
    { path:'listening', canActivate:[authGuard], loadComponent:() => import('./app/features/listening/catalog.component').then(m=>m.ListeningCatalogComponent) },
    { path:'practice/:id', canActivate:[authGuard], loadComponent:() => import('./app/features/reading/session.component').then(m=>m.SessionComponent) },
    { path:'results/:id', canActivate:[authGuard], loadComponent:() => import('./app/features/reading/results.component').then(m=>m.ResultsComponent) },
    { path:'**', redirectTo:'' },
  ])]
}).catch(error => console.error(error));
