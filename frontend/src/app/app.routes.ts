// @ts-ignore
import { Routes } from '@angular/router';

import { LoginComponent } from './components/login/login.component';
import { DashboardComponent } from './components/dashboard/dashboard.component';
import { AdminCoursesComponent } from './components/admin/admin-courses.component';
import { AdminLecturesComponent } from './components/admin/admin-lectures.component';
import { AdminUsersComponent } from './components/admin/admin-users.component';
import { authGuard } from './guards/auth.guard';

export const routes: Routes = [
	{ path: 'login', component: LoginComponent },
	{ path: 'dashboard', component: DashboardComponent, canActivate: [authGuard] },
	{ path: 'admin/courses', component: AdminCoursesComponent, canActivate: [authGuard] },
	{ path: 'admin/lectures', component: AdminLecturesComponent, canActivate: [authGuard] },
	{ path: 'admin/users', component: AdminUsersComponent, canActivate: [authGuard] },
	{ path: '', pathMatch: 'full', redirectTo: 'login' },
	{ path: '**', redirectTo: 'login' }
];
