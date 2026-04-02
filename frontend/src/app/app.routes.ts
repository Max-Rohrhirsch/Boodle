// @ts-ignore
import { Routes } from '@angular/router';

import { LoginComponent } from './components/login/login.component';

export const routes: Routes = [
	{ path: '', pathMatch: 'full', redirectTo: 'login' },
	{ path: 'login', component: LoginComponent }
];
