import React, { useState, useEffect } from 'react';
import { AuthContext, User } from './authContextDef';

const DEFAULT_USER: User = {
  id: '1',
  name: 'Trần Minh Anh',
  role: 'Chủ cửa hàng (Owner)',
  email: 'merchant@freshflow.vn',
  storeName: 'FreshFlow Coffee & Tea (Chi nhánh Q.1)',
};

const AUTH_STORAGE_KEY = 'freshflow_merchant_auth';

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  // Initialize auth from localStorage (defaults to authenticated for smooth testing)
  const [isAuthenticated, setIsAuthenticated] = useState<boolean>(() => {
    const stored = localStorage.getItem(AUTH_STORAGE_KEY);
    return stored !== null ? stored === 'true' : true;
  });

  const [user, setUser] = useState<User | null>(() => (isAuthenticated ? DEFAULT_USER : null));

  useEffect(() => {
    localStorage.setItem(AUTH_STORAGE_KEY, String(isAuthenticated));
    if (isAuthenticated) {
      setUser(DEFAULT_USER);
      localStorage.setItem('freshflow_actor_user_id', DEFAULT_USER.id);
    } else {
      setUser(null);
      localStorage.removeItem('freshflow_actor_user_id');
    }
  }, [isAuthenticated]);

  const login = (userId: string = '1') => {
    setIsAuthenticated(true);
    setUser({ ...DEFAULT_USER, id: userId });
  };

  const logout = () => {
    setIsAuthenticated(false);
    setUser(null);
  };

  return (
    <AuthContext.Provider value={{ isAuthenticated, user, login, logout }}>
      {children}
    </AuthContext.Provider>
  );
};
