import { ReactNode, Children, HTMLAttributes } from 'react';
import { motion, Variants } from 'framer-motion';
import { cn } from '@/lib/utils';

interface StaggeredListProps extends HTMLAttributes<HTMLDivElement> {
  children: ReactNode;
  staggerDelay?: number;
  initialDelay?: number;
}

const itemVariants: Variants = {
  hidden: { 
    opacity: 0, 
    y: 15,
    scale: 0.98,
  },
  visible: { 
    opacity: 1, 
    y: 0,
    scale: 1,
    transition: {
      duration: 0.35,
      ease: [0.25, 0.46, 0.45, 0.94] as [number, number, number, number],
    },
  },
};

export function StaggeredList({
  children,
  staggerDelay = 0.08,
  initialDelay = 0,
  className,
  ...props
}: StaggeredListProps) {
  return (
    <div className={cn(className)} {...props}>
      <motion.div
        variants={{
          hidden: { opacity: 0 },
          visible: {
            opacity: 1,
            transition: {
              staggerChildren: staggerDelay,
              delayChildren: initialDelay,
            },
          },
        }}
        initial="hidden"
        animate="visible"
      >
        {Children.map(children, (child) => (
          <motion.div variants={itemVariants}>
            {child}
          </motion.div>
        ))}
      </motion.div>
    </div>
  );
}

export function StaggeredItem({ children }: { children: ReactNode }) {
  return (
    <motion.div variants={itemVariants}>
      {children}
    </motion.div>
  );
}

export default StaggeredList;
