/** Body di POST /api/auth/login (LoginRequestDto) */
export interface LoginRequest {
  email: string;
  password: string;
}

/** Body di POST /api/auth/register (RegisterRequestDto) */
export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
}

/** Risposta di /login e /register (AuthResponseDto) */
export interface AuthResponse {
  token: string;
  tokenType: string;
  /** Validità del token in secondi */
  expiresIn: number;
  id: number;
  name: string;
  email: string;
  roles: string[];
}

export interface AuthUser {
  id: number;
  name: string;
  email: string;
  roles: string[];
}

/** Formato degli errori restituito dal GlobalExceptionHandler (ErrorResponseDto) */
export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  fieldErrors?: Record<string, string>;
}

/** Errore normalizzato esposto ai componenti */
export type AuthErrorCode = 'INVALID_CREDENTIALS' | 'ALREADY_EXISTS' | 'VALIDATION' | 'SERVER_UNREACHABLE' | 'UNKNOWN';

export interface AuthError {
  code: AuthErrorCode;
  message: string;
  fieldErrors?: Record<string, string>;
}
