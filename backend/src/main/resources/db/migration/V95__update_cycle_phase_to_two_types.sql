-- V95: Update cycle phase enum from 4 values to 2 values
-- Old phases: SHAPING, BETTING, BUILD, COOLDOWN
-- New phases: SHAPING_BUILDING (covers SHAPING + BETTING + BUILD), BETTING_COOLDOWN (covers COOLDOWN)

UPDATE cycle SET phase = 'SHAPING_BUILDING' WHERE phase IN ('SHAPING', 'BETTING', 'BUILD');
UPDATE cycle SET phase = 'BETTING_COOLDOWN' WHERE phase = 'COOLDOWN';
