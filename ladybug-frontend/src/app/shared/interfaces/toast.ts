export interface Toast {
  type: string;
  message: string;
  detailed?: string;
  toastCallback?: ToastCallback;
}

export interface ToastCallback {
  buttonText: string;
  callback: () => void;
}
