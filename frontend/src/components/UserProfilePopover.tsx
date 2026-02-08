import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Mail, Building2, Award, Loader2 } from 'lucide-react';
import { Avatar, AvatarFallback, AvatarImage } from './ui/avatar';
import { Badge } from './ui/badge';
import {
  Popover,
  PopoverContent,
  PopoverTrigger,
} from './ui/popover';
import { Separator } from './ui/separator';
import api from '../services/api';
import { UserProfile } from '../types';

interface UserProfilePopoverProps {
  userId: number;
  displayName: string;
  children: React.ReactNode;
}

/**
 * A popover component that displays comprehensive user profile information
 * when clicking on @mentions in comments.
 */
const UserProfilePopover: React.FC<UserProfilePopoverProps> = ({
  userId,
  displayName,
  children,
}) => {
  const { t } = useTranslation();
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState(false);
  const [isOpen, setIsOpen] = useState(false);

  const fetchProfile = async () => {
    if (profile || loading) return;
    
    setLoading(true);
    setError(false);
    try {
      const response = await api.get<UserProfile>(`/users/${userId}/profile`);
      setProfile(response.data);
    } catch (err) {
      console.error('Failed to fetch user profile:', err);
      setError(true);
    } finally {
      setLoading(false);
    }
  };

  const handleOpenChange = (open: boolean) => {
    setIsOpen(open);
    if (open) {
      fetchProfile();
    }
  };

  const getInitials = (name: string) => {
    return name
      .split(' ')
      .map(n => n.charAt(0))
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  return (
    <Popover open={isOpen} onOpenChange={handleOpenChange}>
      <PopoverTrigger asChild>
        {children}
      </PopoverTrigger>
      <PopoverContent className="w-80 p-0" align="start">
        {loading ? (
          <div className="flex items-center justify-center p-6">
            <Loader2 className="h-6 w-6 animate-spin text-muted-foreground" />
          </div>
        ) : error ? (
          <div className="p-4 text-center text-sm text-muted-foreground">
            {t('comments.mention.profileError', 'Failed to load profile')}
          </div>
        ) : profile ? (
          <div>
            {/* Header with avatar and name */}
            <div className="p-4 pb-3">
              <div className="flex items-start gap-3">
                <Avatar className="h-12 w-12">
                  <AvatarImage src={profile.avatarUrl} alt={profile.personName || profile.username} />
                  <AvatarFallback className="text-lg">
                    {getInitials(profile.personName || profile.username)}
                  </AvatarFallback>
                </Avatar>
                <div className="flex-1 min-w-0">
                  <h4 className="font-semibold text-foreground truncate">
                    {profile.personName || profile.username}
                  </h4>
                  <p className="text-sm text-muted-foreground">@{profile.username}</p>
                  <Badge 
                    variant="outline" 
                    className="mt-1 text-xs"
                  >
                    {profile.role.replace('_', ' ')}
                  </Badge>
                </div>
              </div>
            </div>
            
            <Separator />
            
            {/* Profile details */}
            <div className="p-4 pt-3 space-y-3">
              {profile.email && (
                <div className="flex items-center gap-2 text-sm">
                  <Mail className="h-4 w-4 text-muted-foreground flex-shrink-0" />
                  <span className="text-muted-foreground truncate">{profile.email}</span>
                </div>
              )}
              
              {profile.department && (
                <div className="flex items-center gap-2 text-sm">
                  <Building2 className="h-4 w-4 text-muted-foreground flex-shrink-0" />
                  <span className="text-muted-foreground">{profile.department}</span>
                </div>
              )}
              
              {profile.skills && (
                <div className="flex items-start gap-2 text-sm">
                  <Award className="h-4 w-4 text-muted-foreground flex-shrink-0 mt-0.5" />
                  <div className="flex flex-wrap gap-1">
                    {profile.skills.split(',').slice(0, 4).map((skill, i) => (
                      <Badge key={i} variant="secondary" className="text-xs">
                        {skill.trim()}
                      </Badge>
                    ))}
                    {profile.skills.split(',').length > 4 && (
                      <Badge variant="outline" className="text-xs">
                        +{profile.skills.split(',').length - 4}
                      </Badge>
                    )}
                  </div>
                </div>
              )}
              
              {profile.bio && (
                <p className="text-sm text-muted-foreground line-clamp-2">
                  {profile.bio}
                </p>
              )}
              
              {!profile.email && !profile.department && !profile.skills && !profile.bio && (
                <p className="text-sm text-muted-foreground text-center py-2">
                  {t('comments.mention.noDetails', 'No additional details available')}
                </p>
              )}
            </div>
          </div>
        ) : (
          <div className="p-4 text-center text-sm text-muted-foreground">
            {displayName}
          </div>
        )}
      </PopoverContent>
    </Popover>
  );
};

export default UserProfilePopover;
