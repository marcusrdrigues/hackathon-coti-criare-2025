import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { BehaviorSubject, tap } from 'rxjs';
import { Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  // Ajuste a porta se seu Java não for 8080
  private apiUrl = 'http://localhost:8080/auth/login'; 

  private usuarioSubject = new BehaviorSubject<any>(this.getUserFromStorage());
  public usuario$ = this.usuarioSubject.asObservable();

  constructor(private http: HttpClient, private router: Router) { }

  login(credenciais: any) {
    return this.http.post<any>(this.apiUrl, credenciais).pipe(
      tap(resposta => {
        localStorage.setItem('sessao_usuario', JSON.stringify(resposta));
        this.usuarioSubject.next(resposta);
      })
    );
  }

  logout() {
    localStorage.removeItem('sessao_usuario');
    this.usuarioSubject.next(null);
    this.router.navigate(['/pages/login']);
  }

  private getUserFromStorage() {
    if (typeof window !== 'undefined') {
      const user = localStorage.getItem('sessao_usuario');
      return user ? JSON.parse(user) : null;
    }
    return null;
  }
}