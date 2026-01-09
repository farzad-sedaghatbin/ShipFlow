import * as React from "react"
import { cva, type VariantProps } from "class-variance-authority"
import { AlertCircle, CheckCircle2, Info, XCircle } from "lucide-react"

import { cn } from "@/lib/utils"

const alertVariants = cva(
  "relative w-full rounded-lg border p-4 [&>svg~*]:pl-7 [&>svg+div]:translate-y-[-3px] [&>svg]:absolute [&>svg]:left-4 [&>svg]:top-4 [&>svg]:text-foreground",
  {
    variants: {
      variant: {
        default: "bg-background text-foreground",
        destructive:
          "border-destructive/50 text-destructive dark:border-destructive [&>svg]:text-destructive",
        success:
          "border-success/50 text-success dark:border-success [&>svg]:text-success",
        warning:
          "border-warning/50 text-warning dark:border-warning [&>svg]:text-warning",
        info:
          "border-info/50 text-info dark:border-info [&>svg]:text-info",
      },
    },
    defaultVariants: {
      variant: "default",
    },
  }
)

const Alert = React.forwardRef<
  HTMLDivElement,
  React.HTMLAttributes<HTMLDivElement> & VariantProps<typeof alertVariants>
>(({ className, variant, ...props }, ref) => (
  <div
    ref={ref}
    role="alert"
    className={cn(alertVariants({ variant }), className)}
    {...props}
  />
))
Alert.displayName = "Alert"

const AlertTitle = React.forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLHeadingElement>
>(({ className, ...props }, ref) => (
  <h5
    ref={ref}
    className={cn("mb-1 font-medium leading-none tracking-tight", className)}
    {...props}
  />
))
AlertTitle.displayName = "AlertTitle"

const AlertDescription = React.forwardRef<
  HTMLParagraphElement,
  React.HTMLAttributes<HTMLParagraphElement>
>(({ className, ...props }, ref) => (
  <div
    ref={ref}
    className={cn("text-sm [&_p]:leading-relaxed", className)}
    {...props}
  />
))
AlertDescription.displayName = "AlertDescription"

// Convenience components with icons
const AlertSuccess = React.forwardRef<
  HTMLDivElement,
  Omit<React.HTMLAttributes<HTMLDivElement>, "variant">
>(({ className, children, ...props }, ref) => (
  <Alert ref={ref} variant="success" className={className} {...props}>
    <CheckCircle2 className="h-4 w-4" />
    {children}
  </Alert>
))
AlertSuccess.displayName = "AlertSuccess"

const AlertDestructive = React.forwardRef<
  HTMLDivElement,
  Omit<React.HTMLAttributes<HTMLDivElement>, "variant">
>(({ className, children, ...props }, ref) => (
  <Alert ref={ref} variant="destructive" className={className} {...props}>
    <XCircle className="h-4 w-4" />
    {children}
  </Alert>
))
AlertDestructive.displayName = "AlertDestructive"

const AlertWarning = React.forwardRef<
  HTMLDivElement,
  Omit<React.HTMLAttributes<HTMLDivElement>, "variant">
>(({ className, children, ...props }, ref) => (
  <Alert ref={ref} variant="warning" className={className} {...props}>
    <AlertCircle className="h-4 w-4" />
    {children}
  </Alert>
))
AlertWarning.displayName = "AlertWarning"

const AlertInfo = React.forwardRef<
  HTMLDivElement,
  Omit<React.HTMLAttributes<HTMLDivElement>, "variant">
>(({ className, children, ...props }, ref) => (
  <Alert ref={ref} variant="info" className={className} {...props}>
    <Info className="h-4 w-4" />
    {children}
  </Alert>
))
AlertInfo.displayName = "AlertInfo"

export {
  Alert,
  AlertTitle,
  AlertDescription,
  AlertSuccess,
  AlertDestructive,
  AlertWarning,
  AlertInfo,
}
