import { motion, HTMLMotionProps } from 'framer-motion';
import { ReactNode } from 'react';
import { cn } from '@/lib/utils';

interface MotionContainerProps extends Omit<HTMLMotionProps<'div'>, 'children'> {
    children: ReactNode;
    delay?: number;
}

export default function MotionContainer({ children, delay = 0, className, ...props }: MotionContainerProps) {
    return (
        <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.5, delay }}
            className={cn(className)}
            {...props}
        >
            {children}
        </motion.div>
    );
}
