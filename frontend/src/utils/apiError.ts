import axios from 'axios';

export function getApiErrorMessage(error: unknown, fallback: string) {
  if (axios.isAxiosError(error) && error.response?.data) {
    const message = typeof error.response.data.message === 'string' ? error.response.data.message : null;
    const details = Array.isArray(error.response.data.details)
      ? error.response.data.details.filter((item: unknown): item is string => typeof item === 'string')
      : [];

    if (message) {
      return details.length > 0 ? `${message} (${details.join(', ')})` : message;
    }
  }

  return fallback;
}
