'use client'

import { motion } from 'framer-motion'
import { useEffect, useState } from 'react'

export default function Home() {
  const [typedText, setTypedText] = useState('')
  const fullText = 'Transform Your Voice Into Action'

  useEffect(() => {
    let index = 0
    const timer = setInterval(() => {
      if (index <= fullText.length) {
        setTypedText(fullText.substring(0, index))
        index++
      } else {
        clearInterval(timer)
      }
    }, 100)
    return () => clearInterval(timer)
  }, [])

  const features = [
    {
      icon: '🎤',
      title: 'Instant Voice Capture',
      description: 'Press F8 anytime to start recording. Your voice is instantly captured in crystal-clear quality.',
      color: 'from-blue-500 to-cyan-500',
    },
    {
      icon: '🤖',
      title: 'GPT-4 Powered Intelligence',
      description: 'Leverages OpenAI\'s most advanced AI model to understand context and provide intelligent, human-like responses.',
      color: 'from-purple-500 to-pink-500',
    },
    {
      icon: '🔊',
      title: 'Natural Voice Output',
      description: 'Hear responses in natural-sounding speech with OpenAI\'s state-of-the-art text-to-speech technology.',
      color: 'from-green-500 to-teal-500',
    },
    {
      icon: '⚙️',
      title: 'Multi-Device Support',
      description: 'Choose any microphone or speaker. Seamlessly switch between devices from the system tray menu.',
      color: 'from-orange-500 to-red-500',
    },
    {
      icon: '💬',
      title: 'Conversation Memory',
      description: 'Maintains context across conversations for more relevant and personalized interactions.',
      color: 'from-indigo-500 to-purple-500',
    },
    {
      icon: '🎯',
      title: 'Always Available',
      description: 'Runs silently in your system tray. Always ready, never in your way. Zero configuration needed.',
      color: 'from-rose-500 to-pink-500',
    },
  ]

  const benefits = [
    {
      title: '10x Faster Workflow',
      description: 'Skip typing and get instant answers while keeping your hands on the keyboard.',
    },
    {
      title: 'Hands-Free Productivity',
      description: 'Perfect for multitasking, coding, or when you need quick information without interrupting your flow.',
    },
    {
      title: 'Intelligent Conversations',
      description: 'Not just voice commands - have natural conversations with AI that remembers your context.',
    },
  ]

  return (
    <main className="min-h-screen bg-gradient-to-br from-slate-900 via-purple-900 to-slate-900">
      {/* Hero Section */}
      <section className="relative min-h-screen flex items-center justify-center overflow-hidden">
        {/* Animated background elements */}
        <div className="absolute inset-0 overflow-hidden">
          <motion.div
            className="absolute top-20 left-20 w-72 h-72 bg-blue-500 rounded-full mix-blend-multiply filter blur-xl opacity-20"
            animate={{
              x: [0, 100, 0],
              y: [0, -100, 0],
            }}
            transition={{
              duration: 20,
              repeat: Infinity,
              ease: 'easeInOut',
            }}
          />
          <motion.div
            className="absolute top-40 right-20 w-72 h-72 bg-purple-500 rounded-full mix-blend-multiply filter blur-xl opacity-20"
            animate={{
              x: [0, -100, 0],
              y: [0, 100, 0],
            }}
            transition={{
              duration: 15,
              repeat: Infinity,
              ease: 'easeInOut',
            }}
          />
          <motion.div
            className="absolute -bottom-8 left-1/2 w-72 h-72 bg-pink-500 rounded-full mix-blend-multiply filter blur-xl opacity-20"
            animate={{
              x: [0, 50, 0],
              y: [0, -50, 0],
            }}
            transition={{
              duration: 25,
              repeat: Infinity,
              ease: 'easeInOut',
            }}
          />
        </div>

        <div className="relative z-10 text-center px-4 max-w-5xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.8 }}
          >
            <motion.div
              className="mb-8 inline-block"
              animate={{ y: [0, -20, 0] }}
              transition={{ duration: 3, repeat: Infinity, ease: 'easeInOut' }}
            >
              <div className="relative w-32 h-32 mx-auto">
                <div className="absolute inset-0 bg-gradient-to-br from-blue-500 via-purple-600 to-pink-500 rounded-3xl blur-xl opacity-60 animate-pulse-slow"></div>
                <img 
                  src="/on-demand-ai-voice-chat/icon-full.png" 
                  alt="VoiceAI Commander" 
                  className="relative w-full h-full rounded-3xl shadow-2xl"
                />
              </div>
            </motion.div>

            <h1 className="text-6xl md:text-8xl font-bold text-white mb-6 leading-tight">
              <span className="gradient-text">{typedText}</span>
              <motion.span
                animate={{ opacity: [1, 0] }}
                transition={{ duration: 0.8, repeat: Infinity }}
                className="inline-block w-1 h-16 md:h-24 bg-purple-500 ml-2"
              />
            </h1>

            <p className="text-xl md:text-2xl text-gray-300 mb-12 max-w-3xl mx-auto leading-relaxed">
              Meet <span className="font-bold text-white">VoiceAI Commander</span> - Your intelligent voice assistant 
              powered by GPT-4. Press a hotkey, speak naturally, and get instant AI-powered responses.
            </p>

            <div className="flex flex-col sm:flex-row gap-6 justify-center items-center">
              <motion.a
                href="https://github.com/JohannesRabauer/on-demand-ai-voice-chat/releases/latest"
                target="_blank"
                rel="noopener noreferrer"
                className="group relative inline-flex items-center justify-center px-8 py-4 text-lg font-bold text-white bg-gradient-to-r from-blue-600 to-purple-600 rounded-full overflow-hidden shadow-2xl transition-all hover:shadow-purple-500/50 hover:scale-105"
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
              >
                <span className="relative z-10 flex items-center gap-2">
                  <svg className="w-6 h-6" fill="currentColor" viewBox="0 0 20 20">
                    <path fillRule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clipRule="evenodd" />
                  </svg>
                  Download for Windows
                </span>
                <div className="absolute inset-0 bg-gradient-to-r from-purple-600 to-blue-600 opacity-0 group-hover:opacity-100 transition-opacity" />
              </motion.a>

              <motion.a
                href="https://github.com/JohannesRabauer/on-demand-ai-voice-chat"
                target="_blank"
                rel="noopener noreferrer"
                className="inline-flex items-center justify-center px-8 py-4 text-lg font-bold text-white border-2 border-white/30 rounded-full hover:bg-white/10 transition-all backdrop-blur-sm"
                whileHover={{ scale: 1.05 }}
                whileTap={{ scale: 0.95 }}
              >
                <svg className="w-6 h-6 mr-2" fill="currentColor" viewBox="0 0 24 24">
                  <path fillRule="evenodd" d="M12 2C6.477 2 2 6.477 2 12c0 4.42 2.865 8.17 6.839 9.49.5.092.682-.217.682-.482 0-.237-.008-.866-.013-1.7-2.782.603-3.369-1.34-3.369-1.34-.454-1.156-1.11-1.463-1.11-1.463-.908-.62.069-.608.069-.608 1.003.07 1.531 1.03 1.531 1.03.892 1.529 2.341 1.087 2.91.831.092-.646.35-1.086.636-1.336-2.22-.253-4.555-1.11-4.555-4.943 0-1.091.39-1.984 1.029-2.683-.103-.253-.446-1.27.098-2.647 0 0 .84-.269 2.75 1.025A9.578 9.578 0 0112 6.836c.85.004 1.705.114 2.504.336 1.909-1.294 2.747-1.025 2.747-1.025.546 1.377.203 2.394.1 2.647.64.699 1.028 1.592 1.028 2.683 0 3.842-2.339 4.687-4.566 4.935.359.309.678.919.678 1.852 0 1.336-.012 2.415-.012 2.743 0 .267.18.578.688.48C19.138 20.167 22 16.418 22 12c0-5.523-4.477-10-10-10z" clipRule="evenodd" />
                </svg>
                View on GitHub
              </motion.a>
            </div>

            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 1, duration: 0.8 }}
              className="mt-12 text-gray-400 text-sm"
            >
              <p>✨ Free & Open Source • 🪟 Windows 10/11 • 🚀 No Installation Required</p>
            </motion.div>
          </motion.div>
        </div>

        {/* Scroll indicator */}
        <motion.div
          className="absolute bottom-10 left-1/2 transform -translate-x-1/2"
          animate={{ y: [0, 10, 0] }}
          transition={{ duration: 2, repeat: Infinity }}
        >
          <svg className="w-6 h-6 text-white/50" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 14l-7 7m0 0l-7-7m7 7V3" />
          </svg>
        </motion.div>
      </section>

      {/* Features Section */}
      <section className="py-24 px-4 relative">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6 }}
            className="text-center mb-16"
          >
            <h2 className="text-5xl md:text-6xl font-bold text-white mb-6">
              Powerful Features, <span className="gradient-text">Zero Complexity</span>
            </h2>
            <p className="text-xl text-gray-400 max-w-2xl mx-auto">
              Everything you need for seamless voice-AI interaction, built right in.
            </p>
          </motion.div>

          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
            {features.map((feature, index) => (
              <motion.div
                key={index}
                initial={{ opacity: 0, y: 20 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true }}
                transition={{ duration: 0.6, delay: index * 0.1 }}
                whileHover={{ y: -10 }}
                className="glass-card rounded-2xl p-8 hover:shadow-2xl transition-all group"
              >
                <motion.div
                  className={`text-6xl mb-6 inline-block bg-gradient-to-br ${feature.color} rounded-2xl p-4`}
                  whileHover={{ rotate: 360 }}
                  transition={{ duration: 0.6 }}
                >
                  {feature.icon}
                </motion.div>
                <h3 className="text-2xl font-bold text-white mb-4">{feature.title}</h3>
                <p className="text-gray-400 leading-relaxed">{feature.description}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Benefits Section */}
      <section className="py-24 px-4 bg-black/30">
        <div className="max-w-6xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6 }}
            className="text-center mb-16"
          >
            <h2 className="text-5xl md:text-6xl font-bold text-white mb-6">
              Why Developers <span className="gradient-text">Love It</span>
            </h2>
          </motion.div>

          <div className="grid md:grid-cols-3 gap-8">
            {benefits.map((benefit, index) => (
              <motion.div
                key={index}
                initial={{ opacity: 0, scale: 0.9 }}
                whileInView={{ opacity: 1, scale: 1 }}
                viewport={{ once: true }}
                transition={{ duration: 0.5, delay: index * 0.1 }}
                className="text-center"
              >
                <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 mb-6">
                  <span className="text-3xl font-bold text-white">{index + 1}</span>
                </div>
                <h3 className="text-2xl font-bold text-white mb-4">{benefit.title}</h3>
                <p className="text-gray-400 leading-relaxed">{benefit.description}</p>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="py-24 px-4">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          whileInView={{ opacity: 1, y: 0 }}
          viewport={{ once: true }}
          transition={{ duration: 0.6 }}
          className="max-w-4xl mx-auto text-center glass-card rounded-3xl p-12"
        >
          <h2 className="text-4xl md:text-5xl font-bold text-white mb-6">
            Ready to Transform Your Workflow?
          </h2>
          <p className="text-xl text-gray-300 mb-8">
            Download VoiceAI Commander now. No installation, no sign-up. Just extract and run.
          </p>
          
          <motion.a
            href="https://github.com/JohannesRabauer/on-demand-ai-voice-chat/releases/latest"
            target="_blank"
            rel="noopener noreferrer"
            className="inline-flex items-center justify-center px-10 py-5 text-xl font-bold text-white bg-gradient-to-r from-blue-600 to-purple-600 rounded-full shadow-2xl hover:shadow-purple-500/50 transition-all"
            whileHover={{ scale: 1.05 }}
            whileTap={{ scale: 0.95 }}
          >
            <svg className="w-7 h-7 mr-3" fill="currentColor" viewBox="0 0 20 20">
              <path fillRule="evenodd" d="M3 17a1 1 0 011-1h12a1 1 0 110 2H4a1 1 0 01-1-1zm3.293-7.707a1 1 0 011.414 0L9 10.586V3a1 1 0 112 0v7.586l1.293-1.293a1 1 0 111.414 1.414l-3 3a1 1 0 01-1.414 0l-3-3a1 1 0 010-1.414z" clipRule="evenodd" />
            </svg>
            Download Now - It's Free!
          </motion.a>

          <p className="text-gray-400 mt-6 text-sm">
            Requires: Windows 10/11 • OpenAI API Key • ~70MB disk space
          </p>
        </motion.div>
      </section>

      {/* Footer */}
      <footer className="py-12 px-4 border-t border-white/10">
        <div className="max-w-6xl mx-auto text-center text-gray-400">
          <div className="flex justify-center gap-8 mb-6">
            <a
              href="https://github.com/JohannesRabauer/on-demand-ai-voice-chat"
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-white transition-colors"
            >
              GitHub
            </a>
            <a
              href="https://github.com/JohannesRabauer/on-demand-ai-voice-chat#readme"
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-white transition-colors"
            >
              Documentation
            </a>
            <a
              href="https://github.com/JohannesRabauer/on-demand-ai-voice-chat/releases"
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-white transition-colors"
            >
              Releases
            </a>
          </div>
          <p className="text-sm">
            Built with ❤️ using Quarkus, Java 21, OpenAI GPT-4, and modern web technologies
          </p>
          <p className="text-xs mt-2">
            MIT License • Open Source • Free Forever
          </p>
        </div>
      </footer>
    </main>
  )
}
