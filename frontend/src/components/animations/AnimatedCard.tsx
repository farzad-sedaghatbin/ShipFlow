import { ReactNode, HTMLAttributes } from 'react';
import { motion, Variants, TargetAndTransition } from 'framer-motion';
import { cn } from '@/lib/utils';
import { Card } from '@/components/ui/card';
import { useReducedMotion } from '@/hooks/useReducedMotion';

interface AnimatedCardProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  delay?: number;
  animation?: 'fadeUp' | 'fadeIn' | 'slideLeft' | 'slideRight' | 'scale' | 'stagger';
  hoverEffect?: boolean;
}

const animations: Record<string, Variants> = {
  fadeUp: {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0 },
  },
  fadeIn: {
    hidden: { opacity: 0 },
    visible: { opacity: 1 },
  },
  slideLeft: {
    hidden: { opacity: 0, x: 30 },
    visible: { opacity: 1, x: 0 },
  },
  slideRight: {
    hidden: { opacity: 0, x: -30 },
    visible: { opacity: 1, x: 0 },
  },
  scale: {
    hidden: { opacity: 0, scale: 0.9 },
    visible: { opacity: 1, scale: 1 },
  },
  stagger: {
    hidden: { opacity: 0, y: 20 },
    visible: { opacity: 1, y: 0 },
  },
};

// Reduced motion variants - instant transitions
const reducedMotionAnimations: Record<string, Variants> = {
  fadeUp: {
    hidden: { opacity: 0 },
    visible: { opacity: 1 },
  },
  fadeIn: {
    hidden: { opacity: 0 },
    visible: { opacity: 1 },
  },
  slideLeft: {
    hidden: { opacity: 0 },
    visible: { opacity: 1 },
  },
  slideRight: {
    hidden: { opacity: 0 },
    visible: { opacity: 1 },
  },
  scale: {
    hidden: { opacity: 0 },
    visible: { opacity: 1 },
  },
  stagger: {
    hidden: { opacity: 0 },
    visible: { opacity: 1 },
  },
};

const hoverAnimation: TargetAndTransition = {
  scale: 1.02,
  transition: { duration: 0.2 },
};

export function AnimatedCard({
  children,
  delay = 0,
  animation = 'fadeUp',
  hoverEffect = false,
  className,
  ...props
}: AnimatedCardProps) {
  const prefersReducedMotion = useReducedMotion();
  const selectedAnimations = prefersReducedMotion ? reducedMotionAnimations : animations;
  
  return (
    <motion.div
      className="h-full"
      variants={selectedAnimations[animation]}
      initial="hidden"
      animate="visible"
      whileHover={hoverEffect && !prefersReducedMotion ? hoverAnimation : undefined}
      transition={{
        duration: prefersReducedMotion ? 0 : 0.4,
        delay: prefersReducedMotion ? 0 : delay,
        ease: [0.25, 0.46, 0.45, 0.94] as [number, number, number, number],
      }}
      style={{ cursor: hoverEffect ? 'pointer' : 'default' }}
    >
      <Card className={cn('h-full', className)} {...props}>
        {children}
      </Card>
    </motion.div>
  );
}

export default AnimatedCard;
