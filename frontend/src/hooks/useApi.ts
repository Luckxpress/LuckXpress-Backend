import { useState, useEffect, useCallback } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiService } from '../services/api';
import { ApiResponse, PaginatedResponse } from '../types';

// Generic API hook for GET requests
export function useApiQuery<T>(
  queryKey: (string | number)[],
  url: string,
  options?: {
    enabled?: boolean;
    refetchInterval?: number;
    staleTime?: number;
  }
) {
  return useQuery({
    queryKey,
    queryFn: () => apiService.get<T>(url),
    enabled: options?.enabled !== false,
    refetchInterval: options?.refetchInterval,
    staleTime: options?.staleTime || 5 * 60 * 1000, // 5 minutes default
    select: (data) => data.data,
  });
}

// Hook for paginated API calls
export function usePaginatedQuery<T>(
  queryKey: (string | number)[],
  url: string,
  params?: Record<string, any>,
  options?: {
    enabled?: boolean;
    keepPreviousData?: boolean;
  }
) {
  return useQuery({
    queryKey: [...queryKey, params],
    queryFn: () => apiService.getPaginated<T>(url, params),
    enabled: options?.enabled !== false,
    staleTime: 2 * 60 * 1000, // 2 minutes for paginated data
  });
}

// Generic mutation hook
export function useApiMutation<TData = any, TVariables = any>(
  mutationFn: (variables: TVariables) => Promise<ApiResponse<TData>>,
  options?: {
    onSuccess?: (data: TData, variables: TVariables) => void;
    onError?: (error: Error, variables: TVariables) => void;
    invalidateQueries?: (string | number)[][];
  }
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn,
    onSuccess: (response, variables) => {
      if (response.data) {
        options?.onSuccess?.(response.data, variables);
      }
      
      // Invalidate specified queries
      options?.invalidateQueries?.forEach(queryKey => {
        queryClient.invalidateQueries({ queryKey });
      });
    },
    onError: (error: Error, variables) => {
      options?.onError?.(error, variables);
    },
  });
}

// Hook for async operations with loading state
export function useAsyncOperation<T = any>(
  operation: () => Promise<T>,
  dependencies: any[] = []
) {
  const [data, setData] = useState<T | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<Error | null>(null);

  const execute = useCallback(async () => {
    try {
      setLoading(true);
      setError(null);
      const result = await operation();
      setData(result);
      return result;
    } catch (err) {
      const error = err instanceof Error ? err : new Error('An error occurred');
      setError(error);
      throw error;
    } finally {
      setLoading(false);
    }
  }, dependencies);

  const reset = useCallback(() => {
    setData(null);
    setError(null);
    setLoading(false);
  }, []);

  return {
    data,
    loading,
    error,
    execute,
    reset,
  };
}

// Hook for debounced API calls (useful for search)
export function useDebouncedQuery<T>(
  queryKey: (string | number)[],
  url: string,
  searchTerm: string,
  delay: number = 300
) {
  const [debouncedSearchTerm, setDebouncedSearchTerm] = useState(searchTerm);

  useEffect(() => {
    const timer = setTimeout(() => {
      setDebouncedSearchTerm(searchTerm);
    }, delay);

    return () => clearTimeout(timer);
  }, [searchTerm, delay]);

  return useApiQuery<T>(
    [...queryKey, debouncedSearchTerm],
    `${url}?search=${encodeURIComponent(debouncedSearchTerm)}`,
    {
      enabled: debouncedSearchTerm.length >= 2,
    }
  );
}

// Hook for real-time data that auto-refreshes
export function useRealTimeQuery<T>(
  queryKey: (string | number)[],
  url: string,
  intervalMs: number = 30000
) {
  return useApiQuery<T>(queryKey, url, {
    refetchInterval: intervalMs,
    staleTime: 0, // Always consider stale for real-time data
  });
}
