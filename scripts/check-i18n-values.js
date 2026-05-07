const fs = require('fs');
const en = JSON.parse(fs.readFileSync('frontend/src/i18n/locales/en.json', 'utf8'));
const fa = JSON.parse(fs.readFileSync('frontend/src/i18n/locales/fa.json', 'utf8'));

function getVal(obj, path) {
  return path.split('.').reduce((o, k) => (o && o[k] !== undefined ? o[k] : undefined), obj);
}

const missingFromFa = [
  'common.download',
  'cycles.betting_cooldown',
  'pitches.all','pitches.ideas','pitches.shaping','pitches.readyForBetting',
  'pitches.noIdeas','pitches.noDrafts','pitches.noShaped','pitches.quickAddIdea',
  'pitches.quickAddIdeaDescription','pitches.ideaCreated','pitches.addIdea','pitches.addIdeaHint',
  'pitches.titleRequired','pitches.titlePlaceholder','pitches.ideaDescriptionPlaceholder',
  'pitches.shapingStarted','pitches.startShaping','pitches.shape','pitches.noCycleAssigned',
  'pitches.createError','pitches.updateError',
  'pitches.status.idea','pitches.status.draft','pitches.status.in_progress',
  'pitch.statuses.idea','pitch.statuses.draft',
  'status.idea','status.draft',
  'landingPage.wiseArchitecture','landingPage.wiseArchitectureDesc',
  'helpGuidesPage.wiseArchitecture','helpGuidesPage.wiseArchitectureDesc',
  'landing.scopeTaskBridge','landing.scopeTaskBridgeDesc',
  'workLogForm.entries',
  'wiseArchitecture.history.subtitle','wiseArchitecture.history.yourHistory',
  'wiseArchitecture.history.yourHistoryDesc','wiseArchitecture.history.noHistoryYet',
  'wiseArchitecture.history.refresh','wiseArchitecture.history.message',
  'wiseArchitecture.history.messages_one','wiseArchitecture.history.messages_other',
  'wiseArchitecture.history.started','wiseArchitecture.history.initialSolution',
  'wiseArchitecture.history.followUp','wiseArchitecture.history.yourQuestion',
  'wiseArchitecture.history.yourFeedback','wiseArchitecture.history.rateAdvice',
  'wiseArchitecture.history.continueInWise','wiseArchitecture.history.goToWise',
  'wiseArchitecture.history.previous','wiseArchitecture.history.next',
  'wiseArchitecture.history.pageOf',
  'wiseArchitecture.history.feedback.description','wiseArchitecture.history.feedback.helpful',
  'wiseArchitecture.history.feedback.notHelpful',
];

const missingFromEn = [
  'cycles.betting','cycles.build','cycles.cooldown',
  'landing.wiseArchitecture','landing.wiseArchitectureDesc',
  'pitches.status.inProgress',
];

const missingFromBoth = [
  'common.pitch','common.saving','common.noResults','common.new',
  'backlogPage.hillChartAutoCreate','backlogPage.searchPersons',
  'scopeNarrative.autoProgress','scopeNarrative.autoProgressDesc','scopeNarrative.linkedTask',
  'scopeNarrative.linkedTaskDesc','scopeNarrative.suggestedPosition',
  'cooldownActivity.createError','cooldownActivity.updateError',
  'organizationSettings.capacityDesc','organizationSettings.defaultHoursPerDayNote',
  'organizationSettings.defaultWorkingDaysPerWeekNote',
  'releases.nameVersionRequired','releases.updated','releases.createError','releases.noCyclesAvailable',
  'meetingList.table.viewTooltip',
  'peopleManagement.tryDifferentSearch',
  'pitchBoard.cycleRequired','pitchHillChart.taskAutoCreate','conversation',
];

console.log('=== EN values for FA-missing keys ===');
for (const k of missingFromFa) {
  const v = getVal(en, k);
  console.log(k + ' = ' + JSON.stringify(v));
}
console.log('\n=== FA values for EN-missing keys ===');
for (const k of missingFromEn) {
  const v = getVal(fa, k);
  console.log(k + ' = ' + JSON.stringify(v));
}
console.log('\n=== BOTH missing - check current state ===');
for (const k of missingFromBoth) {
  const ev = getVal(en, k);
  const fv = getVal(fa, k);
  console.log(k + ' | EN:' + JSON.stringify(ev) + ' FA:' + JSON.stringify(fv));
}

// Also print the existing ends of sections we need to modify
console.log('\n=== Current section neighbor keys for insertion points ===');
const sections = {
  'en.common': Object.keys(en.common || {}),
  'en.cycles': Object.keys(en.cycles || {}),
  'en.pitches': Object.keys(en.pitches || {}),
  'en.pitches.status': Object.keys((en.pitches || {}).status || {}),
  'en.pitch.statuses': Object.keys(((en.pitch || {}).statuses) || {}),
  'en.releases': Object.keys(en.releases || {}),
  'en.backlogPage': Object.keys(en.backlogPage || {}),
  'en.scopeNarrative': Object.keys(en.scopeNarrative || {}),
  'en.cooldownActivity': Object.keys(en.cooldownActivity || {}),
  'en.organizationSettings': Object.keys(en.organizationSettings || {}),
  'en.meetingList.table': Object.keys(((en.meetingList || {}).table) || {}),
  'en.peopleManagement': Object.keys(en.peopleManagement || {}),
  'en.pitchBoard': Object.keys(en.pitchBoard || {}),
  'en.pitchHillChart': Object.keys(en.pitchHillChart || {}),
  'en.landing': Object.keys(en.landing || {}),
  'en.landingPage': Object.keys(en.landingPage || {}),
  'en.helpGuidesPage': Object.keys(en.helpGuidesPage || {}),
  'en.workLogForm': Object.keys(en.workLogForm || {}),
  'en.wiseArchitecture.history': Object.keys(((en.wiseArchitecture || {}).history) || {}),
  'en.wiseArchitecture.history.feedback': Object.keys((((en.wiseArchitecture || {}).history) || {}).feedback || {}),
};
for (const [name, keys] of Object.entries(sections)) {
  console.log(name + ': [' + keys.join(', ') + ']');
}
