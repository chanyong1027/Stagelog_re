import { Component, type ErrorInfo, type ReactNode } from 'react';
import ErrorState from './ErrorState';

interface Props {
  children: ReactNode;
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
}

interface State {
  error: Error | null;
}

/**
 * 전역 에러 경계. fallback은 ErrorState로 위임하고,
 * '다시 시도'는 full reload 대신 boundary state만 reset한다.
 */
class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    error: null,
  };

  public static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    this.props.onError?.(error, errorInfo);
    console.error('Uncaught error:', error, errorInfo);
  }

  private reset = () => {
    this.setState({ error: null });
  };

  public render() {
    if (this.state.error) {
      return <ErrorState onRetry={this.reset} />;
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
