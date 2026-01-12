import React, { useRef, useEffect, useState, useCallback } from 'react';
import { useTheme } from '../contexts';
import { Card, CardContent } from './ui/card';
import { HillChartPoint } from '../types';

// Sound effects for tactile feedback - uses lazy initialization to comply with browser autoplay policy
const createSoundEffect = () => {
  let audioContext: AudioContext | null = null;
  
  const getAudioContext = (): AudioContext | null => {
    if (typeof window === 'undefined') return null;
    if (!audioContext) {
      try {
        audioContext = new (window.AudioContext || (window as unknown as { webkitAudioContext: typeof AudioContext }).webkitAudioContext)();
      } catch {
        return null;
      }
    }
    if (audioContext.state === 'suspended') {
      audioContext.resume().catch(() => {});
    }
    return audioContext;
  };
  
  return {
    playPickup: () => {
      const ctx = getAudioContext();
      if (!ctx) return;
      try {
        const oscillator = ctx.createOscillator();
        const gainNode = ctx.createGain();
        oscillator.connect(gainNode);
        gainNode.connect(ctx.destination);
        oscillator.frequency.setValueAtTime(600, ctx.currentTime);
        oscillator.frequency.exponentialRampToValueAtTime(800, ctx.currentTime + 0.1);
        gainNode.gain.setValueAtTime(0.15, ctx.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.1);
        oscillator.start(ctx.currentTime);
        oscillator.stop(ctx.currentTime + 0.1);
      } catch {}
    },
    playDrop: () => {
      const ctx = getAudioContext();
      if (!ctx) return;
      try {
        const oscillator = ctx.createOscillator();
        const gainNode = ctx.createGain();
        oscillator.connect(gainNode);
        gainNode.connect(ctx.destination);
        oscillator.frequency.setValueAtTime(400, ctx.currentTime);
        oscillator.frequency.exponentialRampToValueAtTime(200, ctx.currentTime + 0.15);
        gainNode.gain.setValueAtTime(0.2, ctx.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.15);
        oscillator.start(ctx.currentTime);
        oscillator.stop(ctx.currentTime + 0.15);
      } catch {}
    },
    playSuccess: () => {
      const ctx = getAudioContext();
      if (!ctx) return;
      try {
        const oscillator = ctx.createOscillator();
        const gainNode = ctx.createGain();
        oscillator.connect(gainNode);
        gainNode.connect(ctx.destination);
        oscillator.frequency.setValueAtTime(523.25, ctx.currentTime);
        oscillator.frequency.setValueAtTime(659.25, ctx.currentTime + 0.1);
        oscillator.frequency.setValueAtTime(783.99, ctx.currentTime + 0.2);
        gainNode.gain.setValueAtTime(0.15, ctx.currentTime);
        gainNode.gain.exponentialRampToValueAtTime(0.01, ctx.currentTime + 0.3);
        oscillator.start(ctx.currentTime);
        oscillator.stop(ctx.currentTime + 0.3);
      } catch {}
    },
  };
};

interface SpringAnimation {
  current: number;
  target: number;
  velocity: number;
}

const springConfig = { stiffness: 180, damping: 20, mass: 1 };

const updateSpring = (spring: SpringAnimation, dt: number): boolean => {
  const { stiffness, damping, mass } = springConfig;
  const displacement = spring.current - spring.target;
  const springForce = -stiffness * displacement;
  const dampingForce = -damping * spring.velocity;
  const acceleration = (springForce + dampingForce) / mass;
  
  spring.velocity += acceleration * dt;
  spring.current += spring.velocity * dt;
  
  return Math.abs(displacement) > 0.1 || Math.abs(spring.velocity) > 0.1;
};

interface HillChartProps {
  points: HillChartPoint[];
  onPointClick?: (point: HillChartPoint) => void;
  onPointUpdate?: (point: HillChartPoint, newPosition: number) => void;
  onPointEdit?: (point: HillChartPoint) => void;
  onPointDelete?: (point: HillChartPoint) => void;
  readonly?: boolean;
  animateEntrance?: boolean;
}

export const HillChart: React.FC<HillChartProps> = ({
  points,
  onPointClick,
  onPointUpdate,
  onPointEdit,
  onPointDelete,
  readonly = false,
  animateEntrance = true,
}) => {
  const { actualMode } = useTheme();
  const isDarkMode = actualMode === 'dark';
  
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const animationRef = useRef<number>();
  const soundEffects = useRef(createSoundEffect());
  const hasInitialized = useRef(false);
  const [canvasOpacity, setCanvasOpacity] = useState(animateEntrance ? 0 : 1);
  
  const [draggingPoint, setDraggingPoint] = useState<HillChartPoint | null>(null);
  const [hoveredPoint, setHoveredPoint] = useState<HillChartPoint | null>(null);
  const [contextMenuPoint, setContextMenuPoint] = useState<HillChartPoint | null>(null);
  const [contextMenuPosition, setContextMenuPosition] = useState<{ x: number; y: number } | null>(null);
  
  const [animatedPositions, setAnimatedPositions] = useState<Map<number, SpringAnimation>>(new Map());
  const [pulsePhase, setPulsePhase] = useState(0);
  const [ripples, setRipples] = useState<{ id: number; x: number; y: number; startTime: number }[]>([]);
  
  // Responsive dimensions
  const [dimensions, setDimensions] = useState({ width: 800, height: 400 });

  // Theme-aware colors
  const colors = {
    hillGradientTop: isDarkMode ? 'rgba(139, 92, 246, 0.1)' : '#FDF8F3',
    hillGradientBottom: isDarkMode ? '#141415' : '#FFFFFF',
    hillStroke: isDarkMode ? '#8B5CF6' : '#7C3AED',
    hillStrokeLight: isDarkMode ? 'rgba(139, 92, 246, 0.3)' : 'rgba(124, 58, 237, 0.3)',
    dividerLine: isDarkMode ? 'rgba(255, 255, 255, 0.2)' : 'rgba(0, 0, 0, 0.2)',
    pointDefault: isDarkMode ? '#8B5CF6' : '#7C3AED',
    pointHovered: isDarkMode ? '#A78BFA' : '#8B5CF6',
    pointDragging: '#F59E0B',
    pointGlow: isDarkMode ? 'rgba(139, 92, 246, 0.3)' : 'rgba(124, 58, 237, 0.3)',
    textLabel: isDarkMode ? '#71717A' : '#52525B',
    textScope: isDarkMode ? '#FAFAFA' : '#0A0A0B',
    canvasBorder: isDarkMode ? 'rgba(39, 39, 42, 0.5)' : 'rgba(0, 0, 0, 0.1)',
    canvasBg: isDarkMode ? '#141415' : '#FFFCF7',
  };

  const WIDTH = dimensions.width;
  const HEIGHT = dimensions.height;
  const MARGIN = Math.min(60, WIDTH * 0.075);
  const POINT_RADIUS = Math.max(8, Math.min(12, WIDTH / 80));
  const POINT_GLOW_RADIUS = POINT_RADIUS * 1.8;

  useEffect(() => {
    if (animateEntrance) {
      const timer = setTimeout(() => setCanvasOpacity(1), 50);
      return () => clearTimeout(timer);
    }
  }, [animateEntrance]);

  // Handle responsive sizing
  useEffect(() => {
    const updateDimensions = () => {
      if (containerRef.current) {
        const containerWidth = containerRef.current.offsetWidth;
        const isMobile = window.innerWidth < 768;
        const width = isMobile ? Math.min(containerWidth - 32, 600) : Math.min(containerWidth, 800);
        const height = isMobile ? width * 0.6 : width * 0.5;
        setDimensions({ width, height });
      }
    };

    updateDimensions();
    
    const resizeObserver = new ResizeObserver(updateDimensions);
    if (containerRef.current) {
      resizeObserver.observe(containerRef.current);
    }

    return () => resizeObserver.disconnect();
  }, []);

  useEffect(() => {
    setAnimatedPositions(prev => {
      const next = new Map(prev);
      const isFirstRender = !hasInitialized.current && points.length > 0;
      
      points.forEach((point) => {
        if (!next.has(point.id)) {
          const startPosition = isFirstRender && animateEntrance ? 0 : point.position;
          next.set(point.id, { current: startPosition, target: point.position, velocity: 0 });
        } else {
          const spring = next.get(point.id)!;
          spring.target = point.position;
        }
      });
      
      next.forEach((_, id) => {
        if (!points.find(p => p.id === id)) {
          next.delete(id);
        }
      });
      
      if (isFirstRender) hasInitialized.current = true;
      return next;
    });
  }, [points, animateEntrance]);

  useEffect(() => {
    const interval = setInterval(() => {
      setPulsePhase(p => (p + 0.1) % (Math.PI * 2));
    }, 50);
    return () => clearInterval(interval);
  }, []);

  const getYFromPosition = (position: number): number => {
    const hillBottom = HEIGHT - MARGIN;
    const hillTop = MARGIN + 50;
    const normalized = position / 100;
    const angle = normalized * Math.PI;
    return hillBottom - Math.sin(angle) * (hillBottom - hillTop);
  };

  const getXFromPosition = (position: number): number => {
    return MARGIN + (position / 100) * (WIDTH - 2 * MARGIN);
  };

  const drawHill = useCallback((ctx: CanvasRenderingContext2D) => {
    const centerX = WIDTH / 2;
    const hillBottom = HEIGHT - MARGIN;
    const hillTop = MARGIN + 50;

    ctx.clearRect(0, 0, WIDTH, HEIGHT);

    const bgGradient = ctx.createLinearGradient(0, 0, 0, HEIGHT);
    bgGradient.addColorStop(0, colors.hillGradientTop);
    bgGradient.addColorStop(1, colors.hillGradientBottom);
    ctx.fillStyle = bgGradient;
    ctx.fillRect(0, 0, WIDTH, HEIGHT);

    // Grid lines
    ctx.strokeStyle = isDarkMode ? 'rgba(255, 255, 255, 0.05)' : 'rgba(0, 0, 0, 0.05)';
    ctx.lineWidth = 0.5;
    for (let x = MARGIN; x <= WIDTH - MARGIN; x += 40) {
      ctx.beginPath();
      ctx.setLineDash([2, 4]);
      ctx.moveTo(x, hillTop - 20);
      ctx.lineTo(x, hillBottom + 10);
      ctx.stroke();
    }
    ctx.setLineDash([]);

    // Hill shadow
    ctx.beginPath();
    ctx.strokeStyle = colors.hillStrokeLight;
    ctx.lineWidth = 8;
    ctx.lineCap = 'round';
    for (let x = MARGIN; x <= WIDTH - MARGIN; x++) {
      const normalized = (x - MARGIN) / (WIDTH - 2 * MARGIN);
      const angle = normalized * Math.PI;
      const y = hillBottom - Math.sin(angle) * (hillBottom - hillTop) + 3;
      if (x === MARGIN) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    ctx.stroke();

    // Main hill
    ctx.beginPath();
    ctx.strokeStyle = colors.hillStroke;
    ctx.lineWidth = 4;
    ctx.lineCap = 'round';
    for (let x = MARGIN; x <= WIDTH - MARGIN; x++) {
      const normalized = (x - MARGIN) / (WIDTH - 2 * MARGIN);
      const angle = normalized * Math.PI;
      const y = hillBottom - Math.sin(angle) * (hillBottom - hillTop);
      if (x === MARGIN) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    ctx.stroke();

    // Center line
    ctx.beginPath();
    ctx.strokeStyle = colors.dividerLine;
    ctx.lineWidth = 2;
    ctx.setLineDash([8, 8]);
    ctx.moveTo(centerX, hillTop - 10);
    ctx.lineTo(centerX, hillBottom + 5);
    ctx.stroke();
    ctx.setLineDash([]);

    // Phase labels
    ctx.fillStyle = colors.textLabel;
    ctx.font = '600 14px Inter, system-ui, sans-serif';
    ctx.textAlign = 'center';
    ctx.fillText('🔍 Figuring Things Out', centerX / 2 + MARGIN / 2, hillBottom + 40);
    ctx.fillText('🚀 Making It Happen', centerX + centerX / 2 - MARGIN / 2, hillBottom + 40);

    // Position markers
    ctx.fillStyle = isDarkMode ? 'rgba(113, 113, 122, 0.7)' : 'rgba(82, 82, 91, 0.7)';
    ctx.font = '500 11px Inter, system-ui, sans-serif';
    [0, 25, 50, 75, 100].forEach(pos => {
      const x = MARGIN + (pos / 100) * (WIDTH - 2 * MARGIN);
      ctx.fillText(`${pos}%`, x, hillBottom + 22);
    });

    // Ripples
    const now = Date.now();
    ripples.forEach(ripple => {
      const elapsed = now - ripple.startTime;
      const duration = 600;
      if (elapsed < duration) {
        const progress = elapsed / duration;
        const radius = progress * 40;
        const opacity = 1 - progress;
        ctx.beginPath();
        ctx.arc(ripple.x, ripple.y, radius, 0, Math.PI * 2);
        ctx.strokeStyle = `rgba(139, 92, 246, ${opacity * 0.5})`;
        ctx.lineWidth = 2;
        ctx.stroke();
      }
    });

    // Points
    animatedPositions.forEach((spring, id) => {
      const point = points.find(p => p.id === id);
      if (!point) return;

      const x = getXFromPosition(spring.current);
      const y = getYFromPosition(spring.current);

      const isHovered = hoveredPoint?.id === point.id;
      const isDragging = draggingPoint?.id === point.id;
      const isActive = isHovered || isDragging;

      if (isActive) {
        const pulseSize = 1 + Math.sin(pulsePhase) * 0.15;
        const glowGradient = ctx.createRadialGradient(x, y, 0, x, y, POINT_GLOW_RADIUS * pulseSize);
        glowGradient.addColorStop(0, colors.pointGlow);
        glowGradient.addColorStop(1, 'transparent');
        ctx.fillStyle = glowGradient;
        ctx.fillRect(x - 30, y - 30, 60, 60);
      }

      // Shadow
      ctx.beginPath();
      ctx.arc(x + 2, y + 2, POINT_RADIUS, 0, Math.PI * 2);
      ctx.fillStyle = 'rgba(0, 0, 0, 0.15)';
      ctx.fill();

      // Point
      ctx.beginPath();
      ctx.arc(x, y, POINT_RADIUS, 0, Math.PI * 2);
      
      let pointColor = colors.pointDefault;
      if (isDragging) pointColor = colors.pointDragging;
      else if (isHovered) pointColor = colors.pointHovered;

      const pointGradient = ctx.createRadialGradient(x - 3, y - 3, 0, x, y, POINT_RADIUS);
      pointGradient.addColorStop(0, pointColor);
      pointGradient.addColorStop(0.7, pointColor);
      pointGradient.addColorStop(1, isDarkMode ? 'rgba(139, 92, 246, 0.8)' : 'rgba(124, 58, 237, 0.8)');
      ctx.fillStyle = pointGradient;
      ctx.fill();

      ctx.strokeStyle = '#FFFFFF';
      ctx.lineWidth = 3;
      ctx.stroke();

      if (isActive) {
        const labelY = y - POINT_RADIUS - 16;
        ctx.font = '600 13px Inter, system-ui, sans-serif';
        const textWidth = ctx.measureText(point.scope).width;
        const padding = 10;
        
        ctx.fillStyle = isDarkMode ? 'rgba(20, 20, 21, 0.95)' : 'rgba(255, 255, 255, 0.95)';
        ctx.beginPath();
        const labelX = x - textWidth / 2 - padding;
        const labelW = textWidth + padding * 2;
        const labelH = 24;
        ctx.roundRect(labelX, labelY - 16, labelW, labelH, 6);
        ctx.fill();
        ctx.strokeStyle = `${pointColor}40`;
        ctx.lineWidth = 1;
        ctx.stroke();

        ctx.fillStyle = colors.textScope;
        ctx.textAlign = 'center';
        ctx.fillText(point.scope, x, labelY - 2);
      }
    });
  }, [points, hoveredPoint, draggingPoint, animatedPositions, pulsePhase, ripples, colors, isDarkMode]);

  useEffect(() => {
    let lastTime = performance.now();
    
    const animate = (time: number) => {
      const dt = Math.min((time - lastTime) / 1000, 0.1);
      lastTime = time;

      let needsUpdate = false;
      
      setAnimatedPositions(prev => {
        const next = new Map(prev);
        next.forEach((spring) => {
          if (updateSpring(spring, dt)) needsUpdate = true;
        });
        return needsUpdate ? new Map(next) : prev;
      });

      setRipples(prev => prev.filter(r => Date.now() - r.startTime < 600));

      const canvas = canvasRef.current;
      if (canvas) {
        const ctx = canvas.getContext('2d');
        if (ctx) drawHill(ctx);
      }

      animationRef.current = requestAnimationFrame(animate);
    };

    animationRef.current = requestAnimationFrame(animate);
    return () => {
      if (animationRef.current) cancelAnimationFrame(animationRef.current);
    };
  }, [drawHill]);

  const getPointAtPosition = (mouseX: number, mouseY: number): HillChartPoint | null => {
    for (const point of points) {
      const spring = animatedPositions.get(point.id);
      const position = spring?.current ?? point.position;
      const x = getXFromPosition(position);
      const y = getYFromPosition(position);

      const dx = mouseX - x;
      const dy = mouseY - y;
      const distance = Math.sqrt(dx * dx + dy * dy);

      if (distance <= POINT_RADIUS + 8) return point;
    }
    return null;
  };

  const getPositionFromX = (x: number): number => {
    const position = ((x - MARGIN) / (WIDTH - 2 * MARGIN)) * 100;
    return Math.max(0, Math.min(100, Math.round(position)));
  };

  const handleMouseMove = (e: React.MouseEvent<HTMLCanvasElement>) => {
    const rect = canvasRef.current?.getBoundingClientRect();
    if (!rect) return;

    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    if (draggingPoint && !readonly && onPointUpdate) {
      const newPosition = getPositionFromX(x);
      setAnimatedPositions(prev => {
        const next = new Map(prev);
        const spring = next.get(draggingPoint.id);
        if (spring) {
          spring.target = newPosition;
          spring.current = newPosition;
        }
        return next;
      });
    } else {
      const point = getPointAtPosition(x, y);
      setHoveredPoint(point);
    }
  };

  const handleMouseDown = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (readonly) return;

    const rect = canvasRef.current?.getBoundingClientRect();
    if (!rect) return;

    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    const point = getPointAtPosition(x, y);
    if (point) {
      setDraggingPoint(point);
      soundEffects.current.playPickup();
    }
  };

  const handleMouseUp = () => {
    if (draggingPoint && onPointUpdate) {
      const spring = animatedPositions.get(draggingPoint.id);
      const newPosition = spring?.current ?? draggingPoint.position;
      
      const x = getXFromPosition(newPosition);
      const y = getYFromPosition(newPosition);
      setRipples(prev => [...prev, { id: Date.now(), x, y, startTime: Date.now() }]);
      
      soundEffects.current.playDrop();
      
      if (Math.abs(newPosition - draggingPoint.position) > 5) {
        setTimeout(() => soundEffects.current.playSuccess(), 150);
      }
      
      onPointUpdate(draggingPoint, Math.round(newPosition));
    }
    setDraggingPoint(null);
  };

  const handleClick = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (readonly) return;

    const rect = canvasRef.current?.getBoundingClientRect();
    if (!rect) return;

    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    const point = getPointAtPosition(x, y);
    if (point && onPointClick) {
      onPointClick(point);
    }
  };

  const handleContextMenu = (e: React.MouseEvent<HTMLCanvasElement>) => {
    e.preventDefault();
    if (readonly) return;

    const rect = canvasRef.current?.getBoundingClientRect();
    if (!rect) return;

    const x = e.clientX - rect.left;
    const y = e.clientY - rect.top;

    const point = getPointAtPosition(x, y);
    if (point) {
      setContextMenuPoint(point);
      setContextMenuPosition({ x: e.clientX, y: e.clientY });
    }
  };

  // Touch event handlers for mobile support
  const handleTouchStart = (e: React.TouchEvent<HTMLCanvasElement>) => {
    if (readonly) return;
    e.preventDefault();

    const rect = canvasRef.current?.getBoundingClientRect();
    if (!rect) return;

    const touch = e.touches[0];
    const x = touch.clientX - rect.left;
    const y = touch.clientY - rect.top;

    const point = getPointAtPosition(x, y);
    if (point) {
      setDraggingPoint(point);
      soundEffects.current.playPickup();
    }
  };

  const handleTouchMove = (e: React.TouchEvent<HTMLCanvasElement>) => {
    if (readonly) return;
    e.preventDefault();

    const rect = canvasRef.current?.getBoundingClientRect();
    if (!rect) return;

    const touch = e.touches[0];
    const x = touch.clientX - rect.left;

    if (draggingPoint && onPointUpdate) {
      const newPosition = getPositionFromX(x);
      setAnimatedPositions(prev => {
        const next = new Map(prev);
        const spring = next.get(draggingPoint.id);
        if (spring) {
          spring.target = newPosition;
          spring.current = newPosition;
        }
        return next;
      });
    }
  };

  const handleTouchEnd = () => {
    if (draggingPoint && onPointUpdate) {
      const spring = animatedPositions.get(draggingPoint.id);
      const newPosition = spring?.current ?? draggingPoint.position;
      
      const x = getXFromPosition(newPosition);
      const y = getYFromPosition(newPosition);
      setRipples(prev => [...prev, { id: Date.now(), x, y, startTime: Date.now() }]);
      
      soundEffects.current.playDrop();
      
      if (Math.abs(newPosition - draggingPoint.position) > 5) {
        setTimeout(() => soundEffects.current.playSuccess(), 150);
      }
      
      onPointUpdate(draggingPoint, Math.round(newPosition));
    }
    setDraggingPoint(null);
  };

  return (
    <Card data-tour="hill-chart" className="bg-gradient-to-br from-card/90 to-card">
      <CardContent className="p-3 sm:p-6 pb-32">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center mb-4 gap-2">
          <div>
            <h3 className="text-base sm:text-lg font-bold flex items-center gap-2">
              ⛰️ Hill Chart
            </h3>
            <p className="text-xs text-muted-foreground">Track progress visually</p>
          </div>
          {!readonly && (
            <span className="text-xs text-muted-foreground bg-primary/10 px-2 sm:px-3 py-1 rounded-full">
              <span className="hidden sm:inline">🎯 Drag points to update • Right-click for options</span>
              <span className="sm:hidden">🎯 Tap & drag to update</span>
            </span>
          )}
        </div>
        
        <div
          ref={containerRef}
          className="flex justify-center items-center relative transition-opacity duration-500 w-full overflow-x-auto"
          style={{ opacity: canvasOpacity }}
        >
          <canvas
            ref={canvasRef}
            width={WIDTH}
            height={HEIGHT}
            onMouseMove={handleMouseMove}
            onMouseDown={handleMouseDown}
            onMouseUp={handleMouseUp}
            onMouseLeave={() => {
              handleMouseUp();
              setHoveredPoint(null);
            }}
            onTouchStart={handleTouchStart}
            onTouchMove={handleTouchMove}
            onTouchEnd={handleTouchEnd}
            onClick={handleClick}
            onContextMenu={handleContextMenu}
            className="rounded-lg sm:rounded-2xl border-2 transition-shadow touch-manipulation max-w-full"
            style={{
              cursor: draggingPoint ? 'grabbing' : hoveredPoint ? 'grab' : 'default',
              borderColor: colors.canvasBorder,
              backgroundColor: colors.canvasBg,
              boxShadow: 'inset 0 2px 8px rgba(0, 0, 0, 0.04)',
            }}
          />
        </div>

        {/* Hover info card - absolute positioned to prevent layout shift */}
        {hoveredPoint && !draggingPoint && (
          <div className="absolute left-1/2 -translate-x-1/2 mt-4 p-4 bg-card border-2 border-primary/30 rounded-xl shadow-xl transition-all animate-in fade-in slide-in-from-top-2 duration-200 pointer-events-none z-10 max-w-md w-full mx-4">
            <p className="font-bold text-base text-primary">{hoveredPoint.scope}</p>
            <p className="text-sm text-foreground mt-2 leading-relaxed">{hoveredPoint.description}</p>
            <div className="flex items-center gap-2 mt-3">
              <span className="text-sm font-bold bg-primary text-primary-foreground px-3 py-1 rounded-md">
                {hoveredPoint.position}%
              </span>
              <span className="text-sm font-medium text-foreground">
                {hoveredPoint.position < 50 ? '🔍 Figuring things out' : '🚀 Making it happen'}
              </span>
            </div>
          </div>
        )}

        {contextMenuPosition && contextMenuPoint && (
          <div
            className="fixed z-50"
            style={{ left: contextMenuPosition.x, top: contextMenuPosition.y }}
          >
            <div className="bg-popover border border-border rounded-lg shadow-lg py-1 min-w-[120px]">
              <button
                className="w-full px-3 py-2 text-sm text-left hover:bg-accent flex items-center gap-2"
                onClick={() => {
                  if (onPointEdit) onPointEdit(contextMenuPoint);
                  setContextMenuPosition(null);
                  setContextMenuPoint(null);
                }}
              >
                ✏️ Edit
              </button>
              <button
                className="w-full px-3 py-2 text-sm text-left hover:bg-accent flex items-center gap-2 text-destructive"
                onClick={() => {
                  if (onPointDelete) onPointDelete(contextMenuPoint);
                  setContextMenuPosition(null);
                  setContextMenuPoint(null);
                }}
              >
                🗑️ Delete
              </button>
            </div>
          </div>
        )}
        
        {/* Click outside to close context menu */}
        {contextMenuPosition && (
          <div
            className="fixed inset-0 z-40"
            onClick={() => {
              setContextMenuPosition(null);
              setContextMenuPoint(null);
            }}
          />
        )}
      </CardContent>
    </Card>
  );
};
