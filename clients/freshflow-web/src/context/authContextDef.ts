import { createContext } from 'react';

export interface User {
  id: string;
  name: string;
  role: string;
  email: string;
  storeName: string;
}

export interface AuthContextType {
  isAuthenticated: boolean;
  user: User | null;
  login: (userId?: string) => void;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextType | undefined>(undefined);
