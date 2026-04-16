import './globals.css';
import { AuthProvider } from '@/lib/auth';

export const metadata = {
  title:       'TransitPass',
  description: 'Multi-tenant transport ticketing platform',
  manifest:    '/manifest.json',
  appleWebApp: { capable: true, statusBarStyle: 'black-translucent', title: 'TransitPass' },
  themeColor:  '#f0a500',
};

export default function RootLayout({ children }) {
  return (
    <html lang="en">
      <head>
        <link rel="apple-touch-icon" href="/icons/icon.svg" />
        <meta name="mobile-web-app-capable" content="yes" />
      </head>
      <body>
        <AuthProvider>{children}</AuthProvider>
      </body>
    </html>
  );
}
