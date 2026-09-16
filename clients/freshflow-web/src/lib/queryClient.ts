import { QueryClient } from '@tanstack/react-query';

/**
 * Singleton TanStack Query client configured with FreshFlow caching rules
 */
export const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 5 * 60 * 1000, // 5 minutes stale time (ADR 02)
      retry: 1, // Only retry once on network error to prevent spamming server
      refetchOnWindowFocus: false, // Prevent distracting refetches during tab switching
    },
  },
});
