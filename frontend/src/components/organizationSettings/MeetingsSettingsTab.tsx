import { useRef, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { Calendar, Plus, Trash2 } from 'lucide-react';
import { Button } from '../ui/button';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '../ui/card';
import { Input } from '../ui/input';
import { Label } from '../ui/label';
import { Switch } from '../ui/switch';
import { ConfirmDialog } from '../ui/confirm-dialog';
import { OrganizationSettings, MeetingTypeConfig, DorDodItem } from '../../types/organizationSettings';
import { useToast } from '../../contexts';

interface MeetingsSettingsTabProps {
  formData: Partial<OrganizationSettings>;
  setFormData: (data: Partial<OrganizationSettings>) => void;
}

export function MeetingsSettingsTab({ formData, setFormData }: MeetingsSettingsTabProps) {
  const { t } = useTranslation();
  const { showToast } = useToast();
  const meetingTypesEndRef = useRef<HTMLDivElement>(null);

  const [deleteMeetingTypeConfirm, setDeleteMeetingTypeConfirm] = useState<{
    open: boolean;
    typeIndex: number | null;
  }>({ open: false, typeIndex: null });

  const handleConfirmDeleteMeetingType = () => {
    if (deleteMeetingTypeConfirm.typeIndex === null) return;
    const newMeetingTypes = formData.meetingTypes?.filter((_, i) => i !== deleteMeetingTypeConfirm.typeIndex) || [];
    setFormData({ ...formData, meetingTypes: newMeetingTypes });
    showToast(t('organizationSettings.meetingTypeDeleted'), 'success');
    setDeleteMeetingTypeConfirm({ open: false, typeIndex: null });
  };

  const addMeetingType = () => {
    const newMeetingTypes = [...(formData.meetingTypes || [])];
    const uniqueId =
      typeof crypto !== 'undefined' && 'randomUUID' in crypto
        ? crypto.randomUUID()
        : `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
    const newName = `CUSTOM_${uniqueId}`;
    newMeetingTypes.push({
      name: newName,
      displayName: t('organizationSettings.newMeetingType'),
      description: '',
      color: '#6b7280',
      isActive: true,
      order: newMeetingTypes.length + 1,
      dorItems: [],
      dodItems: [],
    });
    setFormData({ ...formData, meetingTypes: newMeetingTypes });
    showToast(t('organizationSettings.meetingTypeAdded'), 'success');
    setTimeout(() => {
      meetingTypesEndRef.current?.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }, 100);
  };

  const updateMeetingType = (typeIndex: number, updates: Partial<MeetingTypeConfig>) => {
    const newMeetingTypes = [...(formData.meetingTypes || [])];
    newMeetingTypes[typeIndex] = { ...newMeetingTypes[typeIndex], ...updates };
    setFormData({ ...formData, meetingTypes: newMeetingTypes });
  };

  const addDorItem = (typeIndex: number) => {
    const newMeetingTypes = [...(formData.meetingTypes || [])];
    const meetingType = newMeetingTypes[typeIndex];
    const newDorItems = [...(meetingType.dorItems || [])];
    newDorItems.push({ name: '', description: '', isRequired: false, order: newDorItems.length + 1 });
    newMeetingTypes[typeIndex] = { ...meetingType, dorItems: newDorItems };
    setFormData({ ...formData, meetingTypes: newMeetingTypes });
  };

  const addDodItem = (typeIndex: number) => {
    const newMeetingTypes = [...(formData.meetingTypes || [])];
    const meetingType = newMeetingTypes[typeIndex];
    const newDodItems = [...(meetingType.dodItems || [])];
    newDodItems.push({ name: '', description: '', isRequired: false, order: newDodItems.length + 1 });
    newMeetingTypes[typeIndex] = { ...meetingType, dodItems: newDodItems };
    setFormData({ ...formData, meetingTypes: newMeetingTypes });
  };

  const softDeleteDorItem = (typeIndex: number, actualIndex: number) => {
    const confirmed = window.confirm(t('organizationSettings.confirmDeleteDorDodItem'));
    if (!confirmed) return;
    const newMeetingTypes = [...(formData.meetingTypes || [])];
    const meetingType = newMeetingTypes[typeIndex];
    const newDorItems = [...(meetingType.dorItems || [])];
    newDorItems[actualIndex] = { ...newDorItems[actualIndex], isDeleted: true };
    newMeetingTypes[typeIndex] = { ...meetingType, dorItems: newDorItems };
    setFormData({ ...formData, meetingTypes: newMeetingTypes });
  };

  const softDeleteDodItem = (typeIndex: number, actualIndex: number) => {
    const confirmed = window.confirm(t('organizationSettings.confirmDeleteDorDodItem'));
    if (!confirmed) return;
    const newMeetingTypes = [...(formData.meetingTypes || [])];
    const meetingType = newMeetingTypes[typeIndex];
    const newDodItems = [...(meetingType.dodItems || [])];
    newDodItems[actualIndex] = { ...newDodItems[actualIndex], isDeleted: true };
    newMeetingTypes[typeIndex] = { ...meetingType, dodItems: newDodItems };
    setFormData({ ...formData, meetingTypes: newMeetingTypes });
  };

  const updateDorItem = (typeIndex: number, actualIndex: number, updates: Partial<DorDodItem>) => {
    const newMeetingTypes = [...(formData.meetingTypes || [])];
    const meetingType = newMeetingTypes[typeIndex];
    const newDorItems = [...(meetingType.dorItems || [])];
    newDorItems[actualIndex] = { ...newDorItems[actualIndex], ...updates };
    newMeetingTypes[typeIndex] = { ...meetingType, dorItems: newDorItems };
    setFormData({ ...formData, meetingTypes: newMeetingTypes });
  };

  const updateDodItem = (typeIndex: number, actualIndex: number, updates: Partial<DorDodItem>) => {
    const newMeetingTypes = [...(formData.meetingTypes || [])];
    const meetingType = newMeetingTypes[typeIndex];
    const newDodItems = [...(meetingType.dodItems || [])];
    newDodItems[actualIndex] = { ...newDodItems[actualIndex], ...updates };
    newMeetingTypes[typeIndex] = { ...meetingType, dodItems: newDodItems };
    setFormData({ ...formData, meetingTypes: newMeetingTypes });
  };

  return (
    <>
      <Card>
        <CardHeader className="flex flex-row items-center justify-between">
          <div>
            <CardTitle className="flex items-center gap-2">
              <Calendar className="h-5 w-5" />
              {t('organizationSettings.meetingTypesConfig')}
            </CardTitle>
            <CardDescription>{t('organizationSettings.meetingTypesDesc')}</CardDescription>
          </div>
          <Button type="button" variant="outline" onClick={addMeetingType}>
            <Plus className="h-4 w-4 mr-2" />
            {t('organizationSettings.addMeetingType')}
          </Button>
        </CardHeader>
        <CardContent className="space-y-6">
          {formData.meetingTypes?.map((meetingType, typeIndex) => (
            <div key={typeIndex} className="border rounded-lg p-4 space-y-4">
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-3">
                  <div className="w-4 h-4 rounded" style={{ backgroundColor: meetingType.color }} />
                  <div>
                    <Input
                      value={meetingType.displayName}
                      onChange={(e) => updateMeetingType(typeIndex, { displayName: e.target.value })}
                      className="font-semibold h-8"
                    />
                    <p className="text-xs text-muted-foreground mt-1">{meetingType.name}</p>
                  </div>
                </div>
                <div className="flex items-center gap-2">
                  <input
                    type="color"
                    value={meetingType.color}
                    onChange={(e) => updateMeetingType(typeIndex, { color: e.target.value })}
                    className="w-8 h-8 rounded cursor-pointer"
                  />
                  <div className="flex items-center gap-1">
                    <Switch
                      checked={meetingType.isActive}
                      onCheckedChange={(checked) => updateMeetingType(typeIndex, { isActive: checked })}
                    />
                    <Label className="text-xs">{t('organizationSettings.active')}</Label>
                  </div>
                  <Button
                    type="button"
                    variant="ghost"
                    size="icon"
                    className="text-destructive hover:text-destructive"
                    onClick={() => setDeleteMeetingTypeConfirm({ open: true, typeIndex })}
                    title={t('organizationSettings.deleteMeetingType')}
                  >
                    <Trash2 className="h-4 w-4" />
                  </Button>
                </div>
              </div>

              <Input
                value={meetingType.description}
                onChange={(e) => updateMeetingType(typeIndex, { description: e.target.value })}
                placeholder={t('organizationSettings.meetingTypeDescPlaceholder')}
                className="text-sm"
              />

              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {/* DOR Items */}
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <Label className="text-sm font-medium">{t('organizationSettings.dorItems')}</Label>
                    <Button type="button" variant="outline" size="sm" onClick={() => addDorItem(typeIndex)}>
                      + {t('organizationSettings.addItem')}
                    </Button>
                  </div>
                  <div className="space-y-2 max-h-64 overflow-y-auto">
                    {meetingType.dorItems?.filter(item => !item.isDeleted).map((item, itemIndex) => {
                      const actualIndex = meetingType.dorItems?.findIndex(d => d === item) ?? itemIndex;
                      return (
                        <div key={actualIndex} className="flex items-start gap-2 p-2 border rounded bg-muted/30">
                          <div className="flex-1 space-y-1">
                            <Input
                              value={item.name}
                              onChange={(e) => updateDorItem(typeIndex, actualIndex, { name: e.target.value })}
                              placeholder={t('organizationSettings.itemName')}
                              className="h-7 text-sm"
                            />
                            <Input
                              value={item.description}
                              onChange={(e) => updateDorItem(typeIndex, actualIndex, { description: e.target.value })}
                              placeholder={t('organizationSettings.itemDescription')}
                              className="h-7 text-xs"
                            />
                          </div>
                          <div className="flex flex-col items-center gap-1">
                            <div className="flex items-center gap-1">
                              <Switch
                                checked={item.isRequired}
                                onCheckedChange={(checked) => updateDorItem(typeIndex, actualIndex, { isRequired: checked })}
                              />
                              <span className="text-xs">{t('organizationSettings.required')}</span>
                            </div>
                            <Button
                              type="button"
                              variant="ghost"
                              size="icon-sm"
                              onClick={() => softDeleteDorItem(typeIndex, actualIndex)}
                            >
                              ✕
                            </Button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>

                {/* DOD Items */}
                <div className="space-y-2">
                  <div className="flex items-center justify-between">
                    <Label className="text-sm font-medium">{t('organizationSettings.dodItems')}</Label>
                    <Button type="button" variant="outline" size="sm" onClick={() => addDodItem(typeIndex)}>
                      + {t('organizationSettings.addItem')}
                    </Button>
                  </div>
                  <div className="space-y-2 max-h-64 overflow-y-auto">
                    {meetingType.dodItems?.filter(item => !item.isDeleted).map((item, itemIndex) => {
                      const actualIndex = meetingType.dodItems?.findIndex(d => d === item) ?? itemIndex;
                      return (
                        <div key={actualIndex} className="flex items-start gap-2 p-2 border rounded bg-muted/30">
                          <div className="flex-1 space-y-1">
                            <Input
                              value={item.name}
                              onChange={(e) => updateDodItem(typeIndex, actualIndex, { name: e.target.value })}
                              placeholder={t('organizationSettings.itemName')}
                              className="h-7 text-sm"
                            />
                            <Input
                              value={item.description}
                              onChange={(e) => updateDodItem(typeIndex, actualIndex, { description: e.target.value })}
                              placeholder={t('organizationSettings.itemDescription')}
                              className="h-7 text-xs"
                            />
                          </div>
                          <div className="flex flex-col items-center gap-1">
                            <div className="flex items-center gap-1">
                              <Switch
                                checked={item.isRequired}
                                onCheckedChange={(checked) => updateDodItem(typeIndex, actualIndex, { isRequired: checked })}
                              />
                              <span className="text-xs">{t('organizationSettings.required')}</span>
                            </div>
                            <Button
                              type="button"
                              variant="ghost"
                              size="icon-sm"
                              onClick={() => softDeleteDodItem(typeIndex, actualIndex)}
                            >
                              ✕
                            </Button>
                          </div>
                        </div>
                      );
                    })}
                  </div>
                </div>
              </div>
            </div>
          ))}
          <div ref={meetingTypesEndRef} />
        </CardContent>
      </Card>

      <ConfirmDialog
        open={deleteMeetingTypeConfirm.open}
        onOpenChange={(open) => setDeleteMeetingTypeConfirm({ open, typeIndex: null })}
        title={t('organizationSettings.deleteMeetingTypeTitle')}
        description={t('organizationSettings.confirmDeleteMeetingType')}
        confirmLabel={t('common.delete')}
        cancelLabel={t('common.cancel')}
        onConfirm={handleConfirmDeleteMeetingType}
        variant="destructive"
      />
    </>
  );
}
