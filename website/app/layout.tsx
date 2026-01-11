import type { Metadata } from 'next'
import { Inter } from 'next/font/google'
import './globals.css'

const inter = Inter({ subsets: ['latin'] })

export const metadata: Metadata = {
  title: 'VoiceAI Commander - Transform Your Voice Into Action',
  description: 'Revolutionary AI-powered voice assistant for Windows. Record with a hotkey, get intelligent responses powered by GPT-4, and control your workflow hands-free.',
  keywords: ['AI voice assistant', 'Windows voice control', 'GPT-4', 'OpenAI', 'voice automation', 'productivity'],
  authors: [{ name: 'VoiceAI Commander Team' }],
  openGraph: {
    title: 'VoiceAI Commander - Transform Your Voice Into Action',
    description: 'Revolutionary AI-powered voice assistant for Windows with GPT-4 intelligence',
    type: 'website',
  },
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en">
      <body className={inter.className}>{children}</body>
    </html>
  )
}
