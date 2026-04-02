# Frontend & UX Implementation Guide

## Table of Contents
1. [Animation Libraries & Setup](#animation-libraries)
2. [Gamification Mechanics](#gamification-mechanics)
3. [Mobile Development Strategy](#mobile-development-strategy)
4. [Code Sharing Between Web & Mobile](#code-sharing)
5. [Performance Optimization](#performance-optimization)

---

## 1. Animation Libraries & Setup

### A. Framer Motion (Web Animations)

#### Installation
```bash
cd frontend
npm install framer-motion
```

#### Core Animation Components

##### 1. Animated Progress Ring

```typescript
// src/components/animations/ProgressRing.tsx
import { motion } from 'framer-motion';
import { useEffect, useState } from 'react';

interface ProgressRingProps {
  progress: number; // 0-100
  size?: number;
  strokeWidth?: number;
  color?: string;
  duration?: number;
}

export function ProgressRing({
  progress,
  size = 120,
  strokeWidth = 8,
  color = '#3b82f6',
  duration = 1
}: ProgressRingProps) {
  const [displayProgress, setDisplayProgress] = useState(0);
  const radius = (size - strokeWidth) / 2;
  const circumference = radius * 2 * Math.PI;
  const offset = circumference - (displayProgress / 100) * circumference;

  useEffect(() => {
    setDisplayProgress(progress);
  }, [progress]);

  return (
    <div className="progress-ring" style={{ width: size, height: size }}>
      <svg width={size} height={size}>
        {/* Background circle */}
        <circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="transparent"
          stroke="#e5e7eb"
          strokeWidth={strokeWidth}
        />

        {/* Progress circle */}
        <motion.circle
          cx={size / 2}
          cy={size / 2}
          r={radius}
          fill="transparent"
          stroke={color}
          strokeWidth={strokeWidth}
          strokeLinecap="round"
          strokeDasharray={circumference}
          initial={{ strokeDashoffset: circumference }}
          animate={{ strokeDashoffset: offset }}
          transition={{ duration, ease: 'easeInOut' }}
          transform={`rotate(-90 ${size / 2} ${size / 2})`}
        />
      </svg>

      <div className="progress-text">
        <motion.span
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.3 }}
          className="text-2xl font-bold"
        >
          {Math.round(displayProgress)}%
        </motion.span>
      </div>
    </div>
  );
}
```

##### 2. Animated Number Ticker

```typescript
// src/components/animations/NumberTicker.tsx
import { motion, useSpring, useTransform } from 'framer-motion';
import { useEffect } from 'react';

interface NumberTickerProps {
  value: number;
  prefix?: string;
  suffix?: string;
  decimals?: number;
  duration?: number;
}

export function NumberTicker({
  value,
  prefix = '',
  suffix = '',
  decimals = 2,
  duration = 1
}: NumberTickerProps) {
  const spring = useSpring(0, {
    stiffness: 100,
    damping: 30,
    duration: duration * 1000
  });

  const display = useTransform(spring, (current) =>
    current.toFixed(decimals)
  );

  useEffect(() => {
    spring.set(value);
  }, [spring, value]);

  return (
    <motion.span
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.3 }}
    >
      {prefix}
      <motion.span>{display}</motion.span>
      {suffix}
    </motion.span>
  );
}

// Usage
function AccountBalance({ balance }: { balance: number }) {
  return (
    <div className="account-balance">
      <NumberTicker
        value={balance}
        prefix="$"
        decimals={2}
        duration={1.5}
      />
    </div>
  );
}
```

##### 3. Card Animations

```typescript
// src/components/animations/AnimatedCard.tsx
import { motion } from 'framer-motion';
import { ReactNode } from 'react';

interface AnimatedCardProps {
  children: ReactNode;
  delay?: number;
  className?: string;
}

export function AnimatedCard({
  children,
  delay = 0,
  className = ''
}: AnimatedCardProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20, scale: 0.95 }}
      animate={{ opacity: 1, y: 0, scale: 1 }}
      exit={{ opacity: 0, y: -20, scale: 0.95 }}
      transition={{
        duration: 0.4,
        delay,
        ease: [0.25, 0.1, 0.25, 1]
      }}
      whileHover={{
        scale: 1.02,
        boxShadow: '0 10px 30px rgba(0, 0, 0, 0.1)',
        transition: { duration: 0.2 }
      }}
      className={`animated-card ${className}`}
    >
      {children}
    </motion.div>
  );
}

// Stagger children animation
export function AnimatedCardGrid({ children }: { children: ReactNode }) {
  return (
    <motion.div
      className="card-grid"
      initial="hidden"
      animate="visible"
      variants={{
        visible: {
          transition: {
            staggerChildren: 0.1
          }
        }
      }}
    >
      {children}
    </motion.div>
  );
}
```

##### 4. Celebration Animation (Goal Completion)

```typescript
// src/components/animations/CelebrationOverlay.tsx
import { motion, AnimatePresence } from 'framer-motion';
import confetti from 'canvas-confetti';
import { useEffect } from 'react';

interface CelebrationOverlayProps {
  show: boolean;
  onClose: () => void;
  title: string;
  subtitle?: string;
}

export function CelebrationOverlay({
  show,
  onClose,
  title,
  subtitle
}: CelebrationOverlayProps) {
  useEffect(() => {
    if (show) {
      // Fire confetti
      const duration = 3000;
      const animationEnd = Date.now() + duration;

      const interval = setInterval(() => {
        const timeLeft = animationEnd - Date.now();

        if (timeLeft <= 0) {
          clearInterval(interval);
          return;
        }

        confetti({
          particleCount: 3,
          angle: 60,
          spread: 55,
          origin: { x: 0 },
          colors: ['#3b82f6', '#10b981', '#f59e0b']
        });

        confetti({
          particleCount: 3,
          angle: 120,
          spread: 55,
          origin: { x: 1 },
          colors: ['#3b82f6', '#10b981', '#f59e0b']
        });
      }, 50);

      // Auto close after 3 seconds
      const timeout = setTimeout(onClose, 3000);

      return () => {
        clearInterval(interval);
        clearTimeout(timeout);
      };
    }
  }, [show, onClose]);

  return (
    <AnimatePresence>
      {show && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          className="celebration-overlay"
          onClick={onClose}
        >
          <motion.div
            initial={{ scale: 0, rotate: -180 }}
            animate={{ scale: 1, rotate: 0 }}
            exit={{ scale: 0, rotate: 180 }}
            transition={{
              type: 'spring',
              stiffness: 200,
              damping: 20
            }}
            className="celebration-content"
            onClick={(e) => e.stopPropagation()}
          >
            <motion.div
              initial={{ scale: 0 }}
              animate={{ scale: [0, 1.2, 1] }}
              transition={{ delay: 0.2, duration: 0.5 }}
              className="text-6xl mb-4"
            >
              🎉
            </motion.div>

            <motion.h2
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.3 }}
              className="text-3xl font-bold mb-2"
            >
              {title}
            </motion.h2>

            {subtitle && (
              <motion.p
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                transition={{ delay: 0.4 }}
                className="text-gray-600"
              >
                {subtitle}
              </motion.p>
            )}
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

// Usage
function GoalCard({ goal }: { goal: Goal }) {
  const [showCelebration, setShowCelebration] = useState(false);

  const handleContribution = async (amount: number) => {
    await goalService.addContribution(goal.id, amount);

    // Check if goal is completed
    if (goal.currentAmount + amount >= goal.targetAmount) {
      setShowCelebration(true);
    }
  };

  return (
    <>
      <div className="goal-card">
        {/* Goal content */}
      </div>

      <CelebrationOverlay
        show={showCelebration}
        onClose={() => setShowCelebration(false)}
        title="Goal Completed! 🎯"
        subtitle={`You've reached your ${goal.name} goal!`}
      />
    </>
  );
}
```

### B. Lottie Animations

#### Installation
```bash
npm install lottie-react
```

#### Lottie Component Wrapper

```typescript
// src/components/animations/LottieAnimation.tsx
import Lottie from 'lottie-react';
import { CSSProperties } from 'react';

interface LottieAnimationProps {
  animationData: any;
  loop?: boolean;
  autoplay?: boolean;
  style?: CSSProperties;
  className?: string;
}

export function LottieAnimation({
  animationData,
  loop = true,
  autoplay = true,
  style,
  className
}: LottieAnimationProps) {
  return (
    <Lottie
      animationData={animationData}
      loop={loop}
      autoplay={autoplay}
      style={style}
      className={className}
    />
  );
}

// Usage with loading animation
import loadingAnimation from '@/assets/lottie/loading.json';

function LoadingSpinner() {
  return (
    <LottieAnimation
      animationData={loadingAnimation}
      style={{ width: 200, height: 200 }}
    />
  );
}
```

#### Creating a Growing Plant Visualization

```typescript
// src/components/animations/GrowingPlant.tsx
import { motion } from 'framer-motion';
import { useMemo } from 'react';

interface GrowingPlantProps {
  progress: number; // 0-100
  targetAmount: number;
  currentAmount: number;
}

export function GrowingPlant({
  progress,
  targetAmount,
  currentAmount
}: GrowingPlantProps) {
  // Calculate plant stages based on progress
  const stage = useMemo(() => {
    if (progress < 25) return 'seed';
    if (progress < 50) return 'sprout';
    if (progress < 75) return 'growing';
    if (progress < 100) return 'blooming';
    return 'complete';
  }, [progress]);

  const plantHeight = Math.min(progress * 3, 300);

  return (
    <div className="growing-plant">
      <svg width="200" height="300" viewBox="0 0 200 300">
        {/* Pot */}
        <motion.rect
          x="60"
          y="250"
          width="80"
          height="50"
          fill="#8B4513"
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
        />

        {/* Soil */}
        <motion.ellipse
          cx="100"
          cy="250"
          rx="40"
          ry="10"
          fill="#654321"
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.2 }}
        />

        {/* Stem */}
        {stage !== 'seed' && (
          <motion.line
            x1="100"
            y1="250"
            x2="100"
            y2={250 - plantHeight}
            stroke="#228B22"
            strokeWidth="4"
            initial={{ pathLength: 0 }}
            animate={{ pathLength: 1 }}
            transition={{ duration: 1, ease: 'easeOut' }}
          />
        )}

        {/* Leaves */}
        {stage !== 'seed' && stage !== 'sprout' && (
          <>
            <motion.ellipse
              cx="80"
              cy={250 - plantHeight / 2}
              rx="20"
              ry="30"
              fill="#32CD32"
              initial={{ scale: 0, rotate: -45 }}
              animate={{ scale: 1, rotate: -45 }}
              transition={{ delay: 0.5, duration: 0.5 }}
            />
            <motion.ellipse
              cx="120"
              cy={250 - plantHeight / 2 + 20}
              rx="20"
              ry="30"
              fill="#32CD32"
              initial={{ scale: 0, rotate: 45 }}
              animate={{ scale: 1, rotate: 45 }}
              transition={{ delay: 0.6, duration: 0.5 }}
            />
          </>
        )}

        {/* Flower (when complete) */}
        {stage === 'complete' && (
          <motion.g
            initial={{ scale: 0 }}
            animate={{ scale: 1, rotate: 360 }}
            transition={{
              delay: 1,
              duration: 1,
              rotate: { duration: 2, repeat: Infinity, ease: 'linear' }
            }}
          >
            {[0, 72, 144, 216, 288].map((angle, i) => (
              <ellipse
                key={i}
                cx={100 + 15 * Math.cos((angle * Math.PI) / 180)}
                cy={250 - plantHeight + 15 * Math.sin((angle * Math.PI) / 180)}
                rx="8"
                ry="15"
                fill="#FFD700"
                transform={`rotate(${angle} 100 ${250 - plantHeight})`}
              />
            ))}
            <circle cx="100" cy={250 - plantHeight} r="8" fill="#FFA500" />
          </motion.g>
        )}
      </svg>

      <div className="plant-info">
        <p className="font-bold">{stage.toUpperCase()}</p>
        <p className="text-sm text-gray-600">
          ${currentAmount.toLocaleString()} / ${targetAmount.toLocaleString()}
        </p>
      </div>
    </div>
  );
}
```

### C. Chart Animations (Recharts + Framer Motion)

```typescript
// src/components/charts/AnimatedPieChart.tsx
import { PieChart, Pie, Cell, ResponsiveContainer, Legend, Tooltip } from 'recharts';
import { motion } from 'framer-motion';

interface ChartData {
  name: string;
  value: number;
  color: string;
}

interface AnimatedPieChartProps {
  data: ChartData[];
  title: string;
}

export function AnimatedPieChart({ data, title }: AnimatedPieChartProps) {
  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ duration: 0.5 }}
      className="chart-container"
    >
      <h3 className="text-xl font-bold mb-4">{title}</h3>

      <ResponsiveContainer width="100%" height={300}>
        <PieChart>
          <Pie
            data={data}
            cx="50%"
            cy="50%"
            labelLine={false}
            outerRadius={80}
            fill="#8884d8"
            dataKey="value"
            animationBegin={0}
            animationDuration={800}
            animationEasing="ease-out"
          >
            {data.map((entry, index) => (
              <Cell key={`cell-${index}`} fill={entry.color} />
            ))}
          </Pie>
          <Tooltip />
          <Legend />
        </PieChart>
      </ResponsiveContainer>
    </motion.div>
  );
}

// src/components/charts/AnimatedBarChart.tsx
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
  ResponsiveContainer
} from 'recharts';
import { motion } from 'framer-motion';

interface BarChartData {
  month: string;
  income: number;
  expenses: number;
}

export function AnimatedBarChart({ data }: { data: BarChartData[] }) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 20 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ duration: 0.5 }}
      className="chart-container"
    >
      <ResponsiveContainer width="100%" height={300}>
        <BarChart data={data}>
          <CartesianGrid strokeDasharray="3 3" />
          <XAxis dataKey="month" />
          <YAxis />
          <Tooltip />
          <Legend />
          <Bar
            dataKey="income"
            fill="#10b981"
            animationBegin={0}
            animationDuration={1000}
            animationEasing="ease-out"
          />
          <Bar
            dataKey="expenses"
            fill="#ef4444"
            animationBegin={200}
            animationDuration={1000}
            animationEasing="ease-out"
          />
        </BarChart>
      </ResponsiveContainer>
    </motion.div>
  );
}
```

---

## 2. Gamification Mechanics

### A. Achievement System

#### Achievement Data Structure

```typescript
// src/types/achievements.ts
export interface Achievement {
  id: string;
  title: string;
  description: string;
  icon: string;
  tier: 'bronze' | 'silver' | 'gold' | 'platinum';
  requirement: number;
  category: 'savings' | 'consistency' | 'investment' | 'budgeting';
  unlocked: boolean;
  unlockedAt?: Date;
  progress: number;
}

export const ACHIEVEMENTS: Achievement[] = [
  {
    id: 'first_transaction',
    title: 'Getting Started',
    description: 'Record your first transaction',
    icon: '🌱',
    tier: 'bronze',
    requirement: 1,
    category: 'consistency',
    unlocked: false,
    progress: 0
  },
  {
    id: 'week_streak',
    title: 'Week Warrior',
    description: 'Log transactions for 7 consecutive days',
    icon: '🔥',
    tier: 'silver',
    requirement: 7,
    category: 'consistency',
    unlocked: false,
    progress: 0
  },
  {
    id: 'first_goal',
    title: 'Goal Setter',
    description: 'Create your first savings goal',
    icon: '🎯',
    tier: 'bronze',
    requirement: 1,
    category: 'savings',
    unlocked: false,
    progress: 0
  },
  {
    id: 'goal_completed',
    title: 'Goal Crusher',
    description: 'Complete your first savings goal',
    icon: '🏆',
    tier: 'gold',
    requirement: 1,
    category: 'savings',
    unlocked: false,
    progress: 0
  },
  {
    id: 'budget_keeper',
    title: 'Budget Master',
    description: 'Stay under budget for a month',
    icon: '💰',
    tier: 'silver',
    requirement: 1,
    category: 'budgeting',
    unlocked: false,
    progress: 0
  },
  {
    id: 'investor',
    title: 'Portfolio Builder',
    description: 'Make 10 investment transactions',
    icon: '📈',
    tier: 'gold',
    requirement: 10,
    category: 'investment',
    unlocked: false,
    progress: 0
  }
];
```

#### Achievement Tracking Component

```typescript
// src/components/achievements/AchievementCard.tsx
import { motion } from 'framer-motion';
import { Achievement } from '@/types/achievements';
import { ProgressRing } from '../animations/ProgressRing';

interface AchievementCardProps {
  achievement: Achievement;
  index: number;
}

export function AchievementCard({ achievement, index }: AchievementCardProps) {
  const progress = (achievement.progress / achievement.requirement) * 100;

  const tierColors = {
    bronze: '#CD7F32',
    silver: '#C0C0C0',
    gold: '#FFD700',
    platinum: '#E5E4E2'
  };

  return (
    <motion.div
      initial={{ opacity: 0, scale: 0.9 }}
      animate={{ opacity: 1, scale: 1 }}
      transition={{ delay: index * 0.1 }}
      className={`achievement-card ${achievement.unlocked ? 'unlocked' : 'locked'}`}
      whileHover={{ scale: 1.05 }}
    >
      <div className="achievement-icon">
        <span className={achievement.unlocked ? '' : 'grayscale'}>
          {achievement.icon}
        </span>
      </div>

      <div className="achievement-info">
        <h3 className="font-bold">{achievement.title}</h3>
        <p className="text-sm text-gray-600">{achievement.description}</p>

        {!achievement.unlocked && (
          <div className="progress-container">
            <ProgressRing
              progress={progress}
              size={60}
              strokeWidth={4}
              color={tierColors[achievement.tier]}
            />
            <span className="text-xs">
              {achievement.progress} / {achievement.requirement}
            </span>
          </div>
        )}

        {achievement.unlocked && achievement.unlockedAt && (
          <p className="text-xs text-gray-500">
            Unlocked {new Date(achievement.unlockedAt).toLocaleDateString()}
          </p>
        )}
      </div>

      <div className={`tier-badge tier-${achievement.tier}`}>
        {achievement.tier.toUpperCase()}
      </div>
    </motion.div>
  );
}
```

### B. Streak Tracking

```typescript
// src/hooks/useStreak.ts
import { useState, useEffect } from 'react';
import { streakService } from '@/lib/api/streakService';

export function useStreak() {
  const [streak, setStreak] = useState({
    current: 0,
    longest: 0,
    lastLoggedDate: null as Date | null
  });

  useEffect(() => {
    loadStreak();
  }, []);

  const loadStreak = async () => {
    const data = await streakService.getStreak();
    setStreak(data);
  };

  const logActivity = async () => {
    const updated = await streakService.logActivity();
    setStreak(updated);
  };

  return { streak, logActivity };
}

// src/components/dashboard/StreakDisplay.tsx
import { motion } from 'framer-motion';
import { useStreak } from '@/hooks/useStreak';

export function StreakDisplay() {
  const { streak } = useStreak();

  return (
    <motion.div
      className="streak-display"
      whileHover={{ scale: 1.05 }}
    >
      <div className="streak-icon">🔥</div>

      <div className="streak-info">
        <motion.div
          key={streak.current}
          initial={{ scale: 1.5, color: '#f59e0b' }}
          animate={{ scale: 1, color: '#000000' }}
          className="streak-number"
        >
          {streak.current}
        </motion.div>
        <p className="text-sm">Day Streak</p>
      </div>

      <div className="streak-record">
        <span className="text-xs text-gray-500">
          Best: {streak.longest} days
        </span>
      </div>
    </motion.div>
  );
}
```

### C. Level System

```typescript
// src/lib/gamification/levels.ts
export interface Level {
  level: number;
  title: string;
  minXP: number;
  maxXP: number;
  icon: string;
  perks: string[];
}

export const LEVELS: Level[] = [
  {
    level: 1,
    title: 'Beginner',
    minXP: 0,
    maxXP: 100,
    icon: '🌱',
    perks: ['Basic tracking features']
  },
  {
    level: 2,
    title: 'Novice',
    minXP: 100,
    maxXP: 300,
    icon: '🌿',
    perks: ['Unlock categories', 'Basic reports']
  },
  {
    level: 3,
    title: 'Apprentice',
    minXP: 300,
    maxXP: 600,
    icon: '🌳',
    perks: ['Goal tracking', 'Investment tracking']
  },
  {
    level: 4,
    title: 'Expert',
    minXP: 600,
    maxXP: 1000,
    icon: '🏆',
    perks: ['AI recommendations', 'Advanced analytics']
  },
  {
    level: 5,
    title: 'Master',
    minXP: 1000,
    maxXP: Infinity,
    icon: '👑',
    perks: ['All features', 'Priority support', 'Custom themes']
  }
];

export function calculateLevel(xp: number): Level {
  return LEVELS.find(level => xp >= level.minXP && xp < level.maxXP) || LEVELS[0];
}

export function calculateXPForNextLevel(xp: number): number {
  const currentLevel = calculateLevel(xp);
  return currentLevel.maxXP - xp;
}

// XP earning actions
export const XP_REWARDS = {
  ADD_TRANSACTION: 5,
  CREATE_GOAL: 20,
  COMPLETE_GOAL: 50,
  DAILY_LOGIN: 10,
  WEEK_STREAK: 30,
  MONTH_STREAK: 100,
  STAY_UNDER_BUDGET: 40,
  ADD_INVESTMENT: 15
};
```

---

## 3. Mobile Development Strategy

### A. React Native Setup

#### Project Initialization

```bash
# Create React Native project with Expo
cd "C:\Users\meryem.durgun\Desktop\Projeler\Expense Tracker"
npx create-expo-app mobile --template blank-typescript

cd mobile
npm install @react-navigation/native @react-navigation/bottom-tabs @react-navigation/native-stack
npm install react-native-reanimated react-native-gesture-handler react-native-screens
npm install @tanstack/react-query axios zustand
npm install react-native-svg react-native-safe-area-context
```

#### Project Structure

```
mobile/
├── app/
│   ├── (auth)/
│   │   ├── login.tsx
│   │   └── register.tsx
│   ├── (tabs)/
│   │   ├── _layout.tsx
│   │   ├── index.tsx          # Dashboard
│   │   ├── transactions.tsx
│   │   ├── goals.tsx
│   │   └── profile.tsx
│   └── _layout.tsx
├── components/
│   ├── animations/
│   │   ├── ProgressRing.tsx
│   │   └── NumberTicker.tsx
│   ├── charts/
│   │   └── PieChart.tsx
│   └── ui/
│       ├── Button.tsx
│       ├── Input.tsx
│       └── Card.tsx
├── lib/
│   ├── api/
│   │   └── client.ts
│   ├── hooks/
│   │   └── useAuth.ts
│   └── store/
│       └── authStore.ts
└── constants/
    └── Colors.ts
```

### B. Platform-Specific Features

#### Secure Storage (Mobile)

```typescript
// mobile/src/lib/storage/secureStorage.ts
import * as SecureStore from 'expo-secure-store';

export const secureStorage = {
  async setItem(key: string, value: string): Promise<void> {
    await SecureStore.setItemAsync(key, value);
  },

  async getItem(key: string): Promise<string | null> {
    return await SecureStore.getItemAsync(key);
  },

  async removeItem(key: string): Promise<void> {
    await SecureStore.deleteItemAsync(key);
  }
};

// Token manager for mobile
class MobileTokenManager {
  private readonly ACCESS_TOKEN_KEY = 'access_token';
  private readonly REFRESH_TOKEN_KEY = 'refresh_token';

  async setAccessToken(token: string): Promise<void> {
    await secureStorage.setItem(this.ACCESS_TOKEN_KEY, token);
  }

  async getAccessToken(): Promise<string | null> {
    return await secureStorage.getItem(this.ACCESS_TOKEN_KEY);
  }

  async setRefreshToken(token: string): Promise<void> {
    await secureStorage.setItem(this.REFRESH_TOKEN_KEY, token);
  }

  async getRefreshToken(): Promise<string | null> {
    return await secureStorage.getItem(this.REFRESH_TOKEN_KEY);
  }

  async clearTokens(): Promise<void> {
    await secureStorage.removeItem(this.ACCESS_TOKEN_KEY);
    await secureStorage.removeItem(this.REFRESH_TOKEN_KEY);
  }
}

export const tokenManager = new MobileTokenManager();
```

#### React Native Reanimated Animations

```typescript
// mobile/src/components/animations/ProgressRing.tsx
import Animated, {
  useSharedValue,
  useAnimatedProps,
  withTiming,
  Easing
} from 'react-native-reanimated';
import { Circle, Svg } from 'react-native-svg';
import { useEffect } from 'react';

const AnimatedCircle = Animated.createAnimatedComponent(Circle);

interface ProgressRingProps {
  progress: number; // 0-100
  size: number;
  strokeWidth: number;
  color: string;
}

export function ProgressRing({
  progress,
  size,
  strokeWidth,
  color
}: ProgressRingProps) {
  const animatedProgress = useSharedValue(0);
  const radius = (size - strokeWidth) / 2;
  const circumference = radius * 2 * Math.PI;

  useEffect(() => {
    animatedProgress.value = withTiming(progress, {
      duration: 1000,
      easing: Easing.bezier(0.25, 0.1, 0.25, 1)
    });
  }, [progress]);

  const animatedProps = useAnimatedProps(() => {
    const strokeDashoffset =
      circumference - (animatedProgress.value / 100) * circumference;

    return {
      strokeDashoffset
    };
  });

  return (
    <Svg width={size} height={size}>
      {/* Background circle */}
      <Circle
        cx={size / 2}
        cy={size / 2}
        r={radius}
        stroke="#e5e7eb"
        strokeWidth={strokeWidth}
        fill="transparent"
      />

      {/* Progress circle */}
      <AnimatedCircle
        cx={size / 2}
        cy={size / 2}
        r={radius}
        stroke={color}
        strokeWidth={strokeWidth}
        strokeLinecap="round"
        fill="transparent"
        strokeDasharray={circumference}
        animatedProps={animatedProps}
        rotation="-90"
        origin={`${size / 2}, ${size / 2}`}
      />
    </Svg>
  );
}
```

---

## 4. Code Sharing Between Web & Mobile

### A. Shared Package Structure

```
shared/
├── package.json
├── tsconfig.json
└── src/
    ├── types/
    │   ├── user.ts
    │   ├── transaction.ts
    │   ├── goal.ts
    │   └── investment.ts
    ├── validation/
    │   ├── schemas.ts
    │   └── validators.ts
    ├── utils/
    │   ├── currency.ts
    │   ├── date.ts
    │   └── calculations.ts
    ├── constants/
    │   └── index.ts
    └── api/
        ├── client.ts
        └── services/
            ├── authService.ts
            ├── transactionService.ts
            ├── goalService.ts
            └── investmentService.ts
```

### B. Shared Types

```typescript
// shared/src/types/transaction.ts
export interface Transaction {
  id: string;
  userId: string;
  accountId: string;
  categoryId?: string;
  type: 'income' | 'expense' | 'transfer';
  amount: number;
  currency: string;
  description?: string;
  transactionDate: string;
  createdAt: string;
  updatedAt: string;
}

export interface TransactionCreate {
  accountId: string;
  categoryId?: string;
  type: 'income' | 'expense' | 'transfer';
  amount: number;
  description?: string;
  transactionDate: string;
}

export interface TransactionSummary {
  totalIncome: number;
  totalExpenses: number;
  netAmount: number;
  transactionCount: number;
}
```

### C. Shared Validation

```typescript
// shared/src/validation/schemas.ts
import { z } from 'zod';

export const transactionSchema = z.object({
  accountId: z.string().uuid(),
  amount: z.number().positive().max(9999999999.99),
  type: z.enum(['income', 'expense', 'transfer']),
  description: z.string().max(500).optional(),
  transactionDate: z.string().datetime()
});

export const goalSchema = z.object({
  name: z.string().min(1).max(100),
  targetAmount: z.number().positive().max(9999999999.99),
  targetDate: z.string().datetime().optional(),
  icon: z.string().max(50).optional(),
  color: z.string().regex(/^#[0-9A-Fa-f]{6}$/).optional()
});

// Export types
export type TransactionInput = z.infer<typeof transactionSchema>;
export type GoalInput = z.infer<typeof goalSchema>;
```

### D. Shared API Services

```typescript
// shared/src/api/services/transactionService.ts
import { apiClient } from '../client';
import type { Transaction, TransactionCreate, TransactionSummary } from '../../types/transaction';

export const transactionService = {
  async getAll(filters?: {
    startDate?: string;
    endDate?: string;
    type?: string;
    accountId?: string;
  }): Promise<Transaction[]> {
    const { data } = await apiClient.get('/transactions', { params: filters });
    return data;
  },

  async getById(id: string): Promise<Transaction> {
    const { data } = await apiClient.get(`/transactions/${id}`);
    return data;
  },

  async create(transaction: TransactionCreate): Promise<Transaction> {
    const { data } = await apiClient.post('/transactions', transaction);
    return data;
  },

  async update(id: string, transaction: Partial<TransactionCreate>): Promise<Transaction> {
    const { data } = await apiClient.put(`/transactions/${id}`, transaction);
    return data;
  },

  async delete(id: string): Promise<void> {
    await apiClient.delete(`/transactions/${id}`);
  },

  async getSummary(startDate: string, endDate: string): Promise<TransactionSummary> {
    const { data } = await apiClient.get('/transactions/summary', {
      params: { startDate, endDate }
    });
    return data;
  }
};
```

### E. Using Shared Code in Web

```typescript
// frontend/package.json
{
  "dependencies": {
    "@finance-tracker/shared": "file:../shared"
  }
}

// frontend/src/pages/transactions/TransactionsPage.tsx
import { transactionService } from '@finance-tracker/shared';
import { Transaction } from '@finance-tracker/shared/types';

export function TransactionsPage() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);

  useEffect(() => {
    loadTransactions();
  }, []);

  const loadTransactions = async () => {
    const data = await transactionService.getAll();
    setTransactions(data);
  };

  return (
    <div>
      {transactions.map(tx => (
        <TransactionCard key={tx.id} transaction={tx} />
      ))}
    </div>
  );
}
```

### F. Using Shared Code in Mobile

```typescript
// mobile/package.json
{
  "dependencies": {
    "@finance-tracker/shared": "file:../shared"
  }
}

// mobile/app/(tabs)/transactions.tsx
import { transactionService } from '@finance-tracker/shared';
import { Transaction } from '@finance-tracker/shared/types';
import { FlatList } from 'react-native';

export default function TransactionsScreen() {
  const [transactions, setTransactions] = useState<Transaction[]>([]);

  useEffect(() => {
    loadTransactions();
  }, []);

  const loadTransactions = async () => {
    const data = await transactionService.getAll();
    setTransactions(data);
  };

  return (
    <FlatList
      data={transactions}
      renderItem={({ item }) => <TransactionCard transaction={item} />}
      keyExtractor={item => item.id}
    />
  );
}
```

---

## 5. Performance Optimization

### A. React Performance

#### Memoization

```typescript
// Memoize expensive calculations
import { useMemo } from 'react';

function DashboardPage() {
  const transactions = useTransactions();

  const summary = useMemo(() => {
    return calculateSummary(transactions);
  }, [transactions]);

  const chartData = useMemo(() => {
    return prepareChartData(transactions);
  }, [transactions]);

  return (
    <div>
      <SummaryCard data={summary} />
      <Chart data={chartData} />
    </div>
  );
}

// Memoize components
import { memo } from 'react';

export const TransactionCard = memo(function TransactionCard({ transaction }: Props) {
  return <div>{/* ... */}</div>;
});
```

#### Code Splitting

```typescript
// Lazy load routes
import { lazy, Suspense } from 'react';
import { Routes, Route } from 'react-router-dom';

const Dashboard = lazy(() => import('@/pages/dashboard/DashboardPage'));
const Transactions = lazy(() => import('@/pages/transactions/TransactionsPage'));
const Goals = lazy(() => import('@/pages/goals/GoalsPage'));

export function AppRoutes() {
  return (
    <Suspense fallback={<LoadingSpinner />}>
      <Routes>
        <Route path="/" element={<Dashboard />} />
        <Route path="/transactions" element={<Transactions />} />
        <Route path="/goals" element={<Goals />} />
      </Routes>
    </Suspense>
  );
}
```

### B. Animation Performance

```typescript
// Use transform and opacity for best performance
const cardVariants = {
  hidden: { opacity: 0, y: 20 },
  visible: { opacity: 1, y: 0 }
};

// BAD - triggers layout recalculation
const badVariants = {
  hidden: { height: 0 },
  visible: { height: 'auto' }
};

// GOOD - uses transform (GPU accelerated)
const goodVariants = {
  hidden: { opacity: 0, scaleY: 0 },
  visible: { opacity: 1, scaleY: 1 }
};

// Use will-change for complex animations
<motion.div
  style={{ willChange: 'transform' }}
  animate={{ x: 100 }}
>
```

### C. Image Optimization

```typescript
// Lazy load images
import { LazyLoadImage } from 'react-lazy-load-image-component';

function GoalCard({ goal }: { goal: Goal }) {
  return (
    <div>
      <LazyLoadImage
        src={goal.imageUrl}
        alt={goal.name}
        effect="blur"
        placeholderSrc="/placeholder.jpg"
      />
    </div>
  );
}
```

---

*Continue to Phase 1 Implementation Guide for step-by-step setup instructions.*
