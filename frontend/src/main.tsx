import ReactDOM from 'react-dom/client'
import { BrowserRouter } from 'react-router-dom'
import App from './App'
import { AuthProvider, ToastProvider, ThemeProvider } from './contexts'
import { TooltipProvider } from '@/components/ui/tooltip'
import './index.css'

ReactDOM.createRoot(document.getElementById('root')!).render(
  <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
    <ThemeProvider>
      <TooltipProvider>
        <ToastProvider>
          <AuthProvider>
            <App />
          </AuthProvider>
        </ToastProvider>
      </TooltipProvider>
    </ThemeProvider>
  </BrowserRouter>,
)

