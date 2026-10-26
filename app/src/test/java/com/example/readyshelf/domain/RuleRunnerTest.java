package com.example.readyshelf.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for RuleRunner: how the app calls the rules, sorts their answers, and checks swaps.
 *
 * <p>These tests don't test any date or swap rules. The test helpers below return answers that
 * each test sets up in advance; they are not a DatePolicy or a RotationPlanner.
 */
public class RuleRunnerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 4);
    private static final DateThresholds THRESHOLDS = DateThresholds.of(14, 200);

    private static final BatchInfo BARS_KIT = batch(1, ItemCategory.FOOD,
            LocalDate.of(2026, 10, 9), LocationType.EMERGENCY_KIT, false);
    private static final BatchInfo BARS_PANTRY = batch(2, ItemCategory.FOOD,
            LocalDate.of(2027, 6, 1), LocationType.PANTRY, false);
    private static final BatchInfo WATER_KIT = batch(3, ItemCategory.WATER,
            LocalDate.of(2026, 10, 20), LocationType.EMERGENCY_KIT, false);
    private static final BatchInfo PAIN_RELIEVER = batch(4, ItemCategory.MEDICINE,
            LocalDate.of(2026, 10, 6), LocationType.FIRST_AID, false);
    private static final BatchInfo BANDAGES = batch(5, ItemCategory.FIRST_AID_SUPPLY,
            null, LocationType.FIRST_AID, false);
    private static final BatchInfo SAMPLE_BARS = batch(6, ItemCategory.FOOD,
            LocalDate.of(2027, 1, 1), LocationType.PANTRY, true);
    private static final BatchInfo BARS_KIT_TODAY = batch(7, ItemCategory.FOOD,
            TODAY, LocationType.EMERGENCY_KIT, false);
    private static final BatchInfo BARS_KIT_PASSED = batch(8, ItemCategory.FOOD,
            TODAY.minusDays(1), LocationType.EMERGENCY_KIT, false);
    private static final BatchInfo BARS_KIT_NO_DATE = batch(9, ItemCategory.FOOD,
            null, LocationType.EMERGENCY_KIT, false);

    private static final List<BatchInfo> ALL = Arrays.asList(BARS_KIT, BARS_PANTRY, WATER_KIT,
            PAIN_RELIEVER, BANDAGES, SAMPLE_BARS, BARS_KIT_TODAY, BARS_KIT_PASSED, BARS_KIT_NO_DATE);

    // ----- Date statuses -----

    @Test
    public void withStatuses_givesThePolicyEachDateInput_today_andTheNearDateDays() {
        RecordingPolicy policy = new RecordingPolicy(DateStatus.LATER);

        RuleRunner.withStatuses(Arrays.asList(BARS_KIT, BANDAGES), policy, TODAY, THRESHOLDS);

        assertEquals(Arrays.asList(DateInput.from(BARS_KIT), DateInput.from(BANDAGES)), policy.inputs);
        assertEquals(Arrays.asList(TODAY, TODAY), policy.days);
        assertEquals(Arrays.asList(14, 14), policy.nearDateDays);
    }

    @Test
    public void statusesOfActive_neverGivesAnArchivedBatchToThePolicy() {
        BatchInfo archived = new BatchInfo(70, 0, "Item 70", "", "", "units", null,
                ItemCategory.FOOD, 5, LocalDate.of(2026, 10, 20), null, DateLabel.EXPIRES, true, 2,
                "Earthquake kit", LocationType.EMERGENCY_KIT, null, null, null, null, null,
                true, false);
        RecordingPolicy policy = new RecordingPolicy(DateStatus.NEAR_DATE);

        Map<Long, DateStatus> statuses = RuleRunner.statusesOfActive(
                Arrays.asList(BARS_KIT, archived), policy, TODAY, THRESHOLDS);

        assertEquals(Collections.singletonList(DateInput.from(BARS_KIT)), policy.inputs);
        assertEquals(DateStatus.NEAR_DATE, statuses.get(1L));
        assertFalse(statuses.containsKey(70L));
    }

    @Test
    public void needsReview_keepsEveryStatusButLater_soonestDateFirst() {
        PresetPolicy policy = new PresetPolicy()
                .status(BARS_KIT, DateStatus.NEAR_DATE)
                .status(BARS_PANTRY, DateStatus.LATER)
                .status(WATER_KIT, DateStatus.TODAY)
                .status(PAIN_RELIEVER, DateStatus.PAST_DATE)
                .status(BANDAGES, DateStatus.NEEDS_REVIEW)
                .status(SAMPLE_BARS, DateStatus.LATER);
        List<BatchInfo> batches = Arrays.asList(BARS_KIT, BARS_PANTRY, WATER_KIT, PAIN_RELIEVER,
                BANDAGES, SAMPLE_BARS);

        List<BatchWithStatus> result = RuleRunner.needsReview(batches, policy, TODAY, THRESHOLDS);

        assertEquals(Arrays.asList(
                new BatchWithStatus(PAIN_RELIEVER, DateStatus.PAST_DATE),
                new BatchWithStatus(BARS_KIT, DateStatus.NEAR_DATE),
                new BatchWithStatus(WATER_KIT, DateStatus.TODAY),
                new BatchWithStatus(BANDAGES, DateStatus.NEEDS_REVIEW)), result);
    }

    @Test
    public void needsReview_nullStatus_failsWithAClearMessage() {
        PresetPolicy policy = new PresetPolicy(); // no status set, so statusOf returns null

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> RuleRunner.needsReview(ALL, policy, TODAY, THRESHOLDS));

        assertTrue(error.getMessage().contains("DatePolicy.statusOf returned null"));
    }

    // ----- Swap plans -----

    @Test
    public void plan_givesThePlannerEveryBatchWithItsFoodClass_today_andTheSettingsNumbers() {
        PresetPlanner planner = new PresetPlanner();
        BatchInfo shelfStable = batch(20, 3, ItemCategory.FOOD, 4, LocalDate.of(2026, 10, 20),
                LocationType.EMERGENCY_KIT, FoodClass.SHELF_STABLE_FOOD);
        List<BatchInfo> batches = new ArrayList<>(ALL);
        batches.add(shelfStable);

        RuleRunner.plan(batches, planner, TODAY, THRESHOLDS);

        List<PlanBatch> expected = new ArrayList<>();
        for (BatchInfo batch : batches) {
            expected.add(PlanBatch.from(batch, batch.getFoodClass()));
        }
        assertEquals(expected, planner.received);
        assertEquals(FoodClass.SHELF_STABLE_FOOD, planner.received.get(batches.size() - 1).getFoodClass());
        assertEquals(null, planner.received.get(0).getFoodClass()); // "Not classified"
        assertEquals(TODAY, planner.today);
        assertEquals(14, planner.nearDateDays);
        assertEquals(200, planner.replacementDays);
    }

    @Test
    public void plan_givesThePlannerAListThatCantBeChanged() {
        PresetPlanner planner = new PresetPlanner();

        RuleRunner.plan(ALL, planner, TODAY, THRESHOLDS);

        assertThrows(UnsupportedOperationException.class, () -> planner.received.clear());
    }

    @Test
    public void plan_keepsAProposalThatPassesTheChecks() {
        PresetPlanner planner = new PresetPlanner().proposal(new SwapProposal(1, 2, 3));

        RuleRunner.ShownPlan shown = RuleRunner.plan(ALL, planner, TODAY, THRESHOLDS);

        assertEquals(Collections.singletonList(new RuleRunner.ShownSwap(BARS_KIT, BARS_PANTRY, 3)),
                shown.getSwaps());
    }

    @Test
    public void plan_dropsProposalsThatFailTheSafetyChecks() {
        PresetPlanner planner = new PresetPlanner()
                .proposal(new SwapProposal(4, 2, 1))   // medicine
                .proposal(new SwapProposal(1, 3, 1))   // the "pantry" batch is water in a kit
                .proposal(new SwapProposal(1, 99, 1))  // a batch that wasn't given
                .proposal(new SwapProposal(1, 1, 1))   // the same batch twice
                .proposal(new SwapProposal(7, 2, 1))   // kit batch dated today
                .proposal(new SwapProposal(8, 2, 1))   // kit batch dated yesterday
                .proposal(new SwapProposal(9, 2, 1))   // kit batch with no date
                .proposal(new SwapProposal(1, 6, 1))   // real batch with a sample batch
                .proposal(new SwapProposal(1, 2, 0))   // nothing to move
                .proposal(null);

        RuleRunner.ShownPlan shown = RuleRunner.plan(ALL, planner, TODAY, THRESHOLDS);

        assertEquals(Collections.emptyList(), shown.getSwaps());
    }

    @Test
    public void plan_showsOnlyNotesThatStillNeedReplacement_withTheirBatch() {
        PresetPlanner planner = new PresetPlanner()
                .note(new PlanNote(1, PlanReason.PARTLY_REPLACED, 2))
                .note(new PlanNote(2, PlanReason.SWAP_PROPOSED, 0))
                .note(new PlanNote(99, PlanReason.NO_REPLACEMENT, 4))  // a batch that wasn't given
                .note(null);
        // No kit food dated today or earlier here, so the app's safety check adds nothing.
        List<BatchInfo> batches = Arrays.asList(BARS_KIT, BARS_PANTRY, WATER_KIT, PAIN_RELIEVER);

        RuleRunner.ShownPlan shown = RuleRunner.plan(batches, planner, TODAY, THRESHOLDS);

        assertEquals(Collections.singletonList(
                new RuleRunner.ShownNeed(BARS_KIT, PlanReason.PARTLY_REPLACED, 2)), shown.getNeeds());
    }

    // ----- Swap safety checks added for confirmable swaps (app code, not the student's rules) -----

    @Test
    public void plan_keepsAProposalFromAFirstAidLocation() {
        BatchInfo firstAidFood = batch(30, 0, ItemCategory.FOOD, 5, LocalDate.of(2026, 10, 9),
                LocationType.FIRST_AID, null);
        PresetPlanner planner = new PresetPlanner().proposal(new SwapProposal(30, 2, 1));

        RuleRunner.ShownPlan shown = RuleRunner.plan(Arrays.asList(firstAidFood, BARS_PANTRY),
                planner, TODAY, THRESHOLDS);

        assertEquals(Collections.singletonList(new RuleRunner.ShownSwap(firstAidFood, BARS_PANTRY, 1)),
                shown.getSwaps());
    }

    @Test
    public void plan_dropsProposalsWithBatchesInTheWrongPlaces() {
        BatchInfo pantryAsKit = batch(31, 0, ItemCategory.FOOD, 5, LocalDate.of(2026, 10, 9),
                LocationType.PANTRY, null);
        BatchInfo kitAsPantry = batch(32, 0, ItemCategory.FOOD, 5, LocalDate.of(2027, 6, 1),
                LocationType.EMERGENCY_KIT, null);
        BatchInfo otherPlace = batch(33, 0, ItemCategory.FOOD, 5, LocalDate.of(2027, 6, 1),
                LocationType.OTHER, null);
        PresetPlanner planner = new PresetPlanner()
                .proposal(new SwapProposal(31, 2, 1))   // the "kit" batch is in a pantry
                .proposal(new SwapProposal(1, 32, 1))   // the "pantry" batch is in a kit
                .proposal(new SwapProposal(1, 33, 1));  // the "pantry" batch is in an Other place

        RuleRunner.ShownPlan shown = RuleRunner.plan(
                Arrays.asList(BARS_KIT, BARS_PANTRY, pantryAsKit, kitAsPantry, otherPlace),
                planner, TODAY, THRESHOLDS);

        assertEquals(Collections.emptyList(), shown.getSwaps());
    }

    @Test
    public void plan_dropsAProposalOfTwoDifferentProducts() {
        BatchInfo otherProduct = batch(34, 7, ItemCategory.FOOD, 5, LocalDate.of(2027, 6, 1),
                LocationType.PANTRY, null);
        PresetPlanner planner = new PresetPlanner().proposal(new SwapProposal(1, 34, 1));

        RuleRunner.ShownPlan shown = RuleRunner.plan(Arrays.asList(BARS_KIT, otherProduct),
                planner, TODAY, THRESHOLDS);

        assertEquals(Collections.emptyList(), shown.getSwaps());
    }

    @Test
    public void plan_dropsAProposalWhosePantryBatchIsDatedTodayOrEarlierOrHasNoDate() {
        BatchInfo pantryToday = batch(35, 0, ItemCategory.FOOD, 5, TODAY, LocationType.PANTRY, null);
        BatchInfo pantryPassed = batch(36, 0, ItemCategory.FOOD, 5, TODAY.minusDays(3),
                LocationType.PANTRY, null);
        BatchInfo pantryNoDate = batch(37, 0, ItemCategory.FOOD, 5, null, LocationType.PANTRY, null);
        PresetPlanner planner = new PresetPlanner()
                .proposal(new SwapProposal(1, 35, 1))
                .proposal(new SwapProposal(1, 36, 1))
                .proposal(new SwapProposal(1, 37, 1));

        RuleRunner.ShownPlan shown = RuleRunner.plan(
                Arrays.asList(BARS_KIT, pantryToday, pantryPassed, pantryNoDate),
                planner, TODAY, THRESHOLDS);

        assertEquals(Collections.emptyList(), shown.getSwaps());
    }

    @Test
    public void plan_dropsAProposalForMoreUnitsThanABatchHas() {
        // Both test batches have 5 units.
        PresetPlanner planner = new PresetPlanner().proposal(new SwapProposal(1, 2, 6));

        RuleRunner.ShownPlan shown = RuleRunner.plan(ALL, planner, TODAY, THRESHOLDS);

        assertEquals(Collections.emptyList(), shown.getSwaps());
    }

    @Test
    public void plan_dropsALaterProposalThatWouldUseTheSameUnitsAgain() {
        BatchInfo secondKit = batch(38, 0, ItemCategory.FOOD, 5, LocalDate.of(2026, 10, 12),
                LocationType.EMERGENCY_KIT, null);
        PresetPlanner planner = new PresetPlanner()
                .proposal(new SwapProposal(1, 2, 3))    // 3 of the pantry batch's 5
                .proposal(new SwapProposal(38, 2, 3))   // 3 more would be 6 of 5: dropped
                .proposal(new SwapProposal(38, 2, 2));  // 2 more are exactly the 5: kept

        RuleRunner.ShownPlan shown = RuleRunner.plan(Arrays.asList(BARS_KIT, BARS_PANTRY, secondKit),
                planner, TODAY, THRESHOLDS);

        assertEquals(Arrays.asList(new RuleRunner.ShownSwap(BARS_KIT, BARS_PANTRY, 3),
                new RuleRunner.ShownSwap(secondKit, BARS_PANTRY, 2)), shown.getSwaps());
    }

    @Test
    public void plan_keepsASwapOfEveryCategoryBoughtAgain() {
        // Since 2026-10-22 (the student's choice) the safety check allows food, water, first aid
        // supplies, cosmetics, and Other on both sides.
        ItemCategory[] boughtAgain = {ItemCategory.FOOD, ItemCategory.WATER,
                ItemCategory.FIRST_AID_SUPPLY, ItemCategory.COSMETIC, ItemCategory.OTHER};
        for (ItemCategory category : boughtAgain) {
            BatchInfo kit = batch(80, 7, category, 5, LocalDate.of(2026, 10, 9),
                    LocationType.EMERGENCY_KIT, null);
            BatchInfo pantry = batch(81, 7, category, 5, LocalDate.of(2027, 6, 1),
                    LocationType.PANTRY, null);
            PresetPlanner planner = new PresetPlanner().proposal(new SwapProposal(80, 81, 2));

            RuleRunner.ShownPlan shown = RuleRunner.plan(Arrays.asList(kit, pantry), planner,
                    TODAY, THRESHOLDS);

            assertEquals(category.name(),
                    Collections.singletonList(new RuleRunner.ShownSwap(kit, pantry, 2)),
                    shown.getSwaps());
        }
    }

    @Test
    public void plan_dropsSwapsOfMedicineDocumentsAndSubscriptions_onEitherSide() {
        ItemCategory[] neverSwapped = {ItemCategory.MEDICINE, ItemCategory.DOCUMENT,
                ItemCategory.SUBSCRIPTION};
        for (ItemCategory category : neverSwapped) {
            BatchInfo kit = batch(82, 7, category, 5, LocalDate.of(2026, 10, 9),
                    LocationType.EMERGENCY_KIT, null);
            BatchInfo pantry = batch(83, 7, category, 5, LocalDate.of(2027, 6, 1),
                    LocationType.PANTRY, null);
            BatchInfo foodKit = batch(84, 7, ItemCategory.FOOD, 5, LocalDate.of(2026, 10, 9),
                    LocationType.EMERGENCY_KIT, null);
            BatchInfo foodPantry = batch(85, 7, ItemCategory.FOOD, 5, LocalDate.of(2027, 6, 1),
                    LocationType.PANTRY, null);
            PresetPlanner planner = new PresetPlanner()
                    .proposal(new SwapProposal(82, 83, 1))   // both batches
                    .proposal(new SwapProposal(82, 85, 1))   // only the kit batch
                    .proposal(new SwapProposal(84, 83, 1));  // only the pantry batch

            RuleRunner.ShownPlan shown = RuleRunner.plan(
                    Arrays.asList(kit, pantry, foodKit, foodPantry), planner, TODAY, THRESHOLDS);

            assertEquals(category.name(), Collections.emptyList(), shown.getSwaps());
        }
    }

    // ----- Replacement needs (app code, not the student's rules) -----

    @Test
    public void plan_needs_notesCountOnlyForKitBatchesOfCategoriesBoughtAgain() {
        // These batches are all dated later, so only a note could give them a need. Since
        // 2026-10-22 a note counts for any kit batch of a category the household buys again
        // (here the water in a kit), and never for medicine, documents, or batches outside kits.
        BatchInfo kitDocument = batch(44, 0, ItemCategory.DOCUMENT, 1, LocalDate.of(2027, 1, 1),
                LocationType.EMERGENCY_KIT, null);
        PresetPlanner planner = new PresetPlanner()
                .note(new PlanNote(4, PlanReason.CATEGORY_NOT_SWAPPED, 5))   // medicine, first aid box
                .note(new PlanNote(44, PlanReason.CATEGORY_NOT_SWAPPED, 1))  // a document in a kit
                .note(new PlanNote(2, PlanReason.NO_REPLACEMENT, 5))         // food in the pantry
                .note(new PlanNote(3, PlanReason.NO_REPLACEMENT, 5));        // water in a kit
        List<BatchInfo> batches = Arrays.asList(BARS_KIT, BARS_PANTRY, WATER_KIT, PAIN_RELIEVER,
                kitDocument);

        RuleRunner.ShownPlan shown = RuleRunner.plan(batches, planner, TODAY, THRESHOLDS);

        assertEquals(Collections.singletonList(
                new RuleRunner.ShownNeed(WATER_KIT, PlanReason.NO_REPLACEMENT, 5)), shown.getNeeds());
    }

    @Test
    public void plan_needs_areNeverMoreThanTheBatch_andKeepTheLargestNotePerBatch() {
        PresetPlanner planner = new PresetPlanner()
                .note(new PlanNote(1, PlanReason.PARTLY_REPLACED, 2))
                .note(new PlanNote(1, PlanReason.NO_REPLACEMENT, 99))  // more than the 5 units
                .note(new PlanNote(1, PlanReason.PARTLY_REPLACED, 1));

        RuleRunner.ShownPlan shown = RuleRunner.plan(Arrays.asList(BARS_KIT, BARS_PANTRY), planner,
                TODAY, THRESHOLDS);

        assertEquals(Collections.singletonList(
                new RuleRunner.ShownNeed(BARS_KIT, PlanReason.NO_REPLACEMENT, 5)), shown.getNeeds());
    }

    @Test
    public void plan_needs_kitFoodDatedTodayOrEarlier_needsItsWholeQuantity_evenWithoutANote() {
        PresetPlanner planner = new PresetPlanner(); // no notes at all

        RuleRunner.ShownPlan shown = RuleRunner.plan(ALL, planner, TODAY, THRESHOLDS);

        // Soonest date first: yesterday's batch, then today's. The future kit batch (1), the
        // batch with no date (9), the pantry, water, and medicine get nothing.
        assertEquals(Arrays.asList(
                new RuleRunner.ShownNeed(BARS_KIT_PASSED, PlanReason.DATE_TODAY_OR_PASSED, 5),
                new RuleRunner.ShownNeed(BARS_KIT_TODAY, PlanReason.DATE_TODAY_OR_PASSED, 5)),
                shown.getNeeds());
    }

    @Test
    public void plan_needs_aSmallerNoteForKitFoodDatedTodayIsRaisedToTheWholeQuantity() {
        PresetPlanner planner = new PresetPlanner()
                .note(new PlanNote(7, PlanReason.PARTLY_REPLACED, 2));

        RuleRunner.ShownPlan shown = RuleRunner.plan(Arrays.asList(BARS_KIT_TODAY), planner,
                TODAY, THRESHOLDS);

        assertEquals(Collections.singletonList(
                new RuleRunner.ShownNeed(BARS_KIT_TODAY, PlanReason.DATE_TODAY_OR_PASSED, 5)),
                shown.getNeeds());
    }

    @Test
    public void plan_needs_aFullNoteForKitFoodDatedEarlierIsKeptWithItsReason() {
        PresetPlanner planner = new PresetPlanner()
                .note(new PlanNote(8, PlanReason.NO_REPLACEMENT, 5));

        RuleRunner.ShownPlan shown = RuleRunner.plan(Arrays.asList(BARS_KIT_PASSED), planner,
                TODAY, THRESHOLDS);

        assertEquals(Collections.singletonList(
                new RuleRunner.ShownNeed(BARS_KIT_PASSED, PlanReason.NO_REPLACEMENT, 5)),
                shown.getNeeds());
    }

    @Test
    public void plan_needs_noNeedForExpiredMedicineDocumentsSubscriptions_orAnEmptyBatch() {
        BatchInfo oldMedicine = batch(40, 0, ItemCategory.MEDICINE, 5, TODAY.minusDays(1),
                LocationType.FIRST_AID, null);
        BatchInfo oldDocument = batch(41, 0, ItemCategory.DOCUMENT, 1, TODAY.minusDays(1),
                LocationType.OTHER, null);
        BatchInfo oldSubscription = batch(43, 0, ItemCategory.SUBSCRIPTION, 1, TODAY,
                LocationType.OTHER, null);
        BatchInfo emptyKitFood = batch(42, 0, ItemCategory.FOOD, 0, TODAY.minusDays(1),
                LocationType.EMERGENCY_KIT, null);

        RuleRunner.ShownPlan shown = RuleRunner.plan(
                Arrays.asList(oldMedicine, oldDocument, oldSubscription, emptyKitFood),
                new PresetPlanner(), TODAY, THRESHOLDS);

        assertEquals(Collections.emptyList(), shown.getNeeds());
    }

    @Test
    public void plan_needs_expiredItemsBoughtAgain_inAnyPlace_needTheirWholeQuantity() {
        // Since 2026-10-22 (the student's choice): food, water, first aid supplies, cosmetics,
        // and other items dated today or earlier need replacement wherever they are.
        BatchInfo pantryFood = batch(60, 0, ItemCategory.FOOD, 5, TODAY.minusDays(1),
                LocationType.PANTRY, null);
        BatchInfo kitWater = batch(61, 0, ItemCategory.WATER, 6, TODAY,
                LocationType.EMERGENCY_KIT, null);
        BatchInfo bandages = batch(62, 0, ItemCategory.FIRST_AID_SUPPLY, 2, TODAY.minusDays(3),
                LocationType.FIRST_AID, null);
        BatchInfo sunscreen = batch(63, 0, ItemCategory.COSMETIC, 1, TODAY.minusDays(1),
                LocationType.OTHER, null);
        BatchInfo batteries = batch(64, 0, ItemCategory.OTHER, 8, TODAY, LocationType.PANTRY, null);
        BatchInfo laterFood = batch(65, 0, ItemCategory.FOOD, 5, TODAY.plusDays(1),
                LocationType.PANTRY, null);

        RuleRunner.ShownPlan shown = RuleRunner.plan(
                Arrays.asList(pantryFood, kitWater, bandages, sunscreen, batteries, laterFood),
                new PresetPlanner(), TODAY, THRESHOLDS);

        // Soonest date first; the same date by name ("Item 60" before "Item 63"). The food dated
        // tomorrow gets nothing.
        assertEquals(Arrays.asList(
                new RuleRunner.ShownNeed(bandages, PlanReason.DATE_TODAY_OR_PASSED, 2),
                new RuleRunner.ShownNeed(pantryFood, PlanReason.DATE_TODAY_OR_PASSED, 5),
                new RuleRunner.ShownNeed(sunscreen, PlanReason.DATE_TODAY_OR_PASSED, 1),
                new RuleRunner.ShownNeed(kitWater, PlanReason.DATE_TODAY_OR_PASSED, 6),
                new RuleRunner.ShownNeed(batteries, PlanReason.DATE_TODAY_OR_PASSED, 8)),
                shown.getNeeds());
    }

    @Test
    public void plan_needs_aKitBatchThatASwapCovers_getsNoNeed_butAnExpiredOneDoes() {
        // "Unless there is a swap available": the swap rules note no need for the near-date kit
        // batch they swap, and the expired kit batch can never be swapped.
        PresetPlanner planner = new PresetPlanner()
                .proposal(new SwapProposal(1, 2, 5))
                .note(new PlanNote(1, PlanReason.SWAP_PROPOSED, 0));

        RuleRunner.ShownPlan shown = RuleRunner.plan(
                Arrays.asList(BARS_KIT, BARS_PANTRY, BARS_KIT_PASSED), planner, TODAY, THRESHOLDS);

        assertEquals(Collections.singletonList(
                new RuleRunner.ShownNeed(BARS_KIT_PASSED, PlanReason.DATE_TODAY_OR_PASSED, 5)),
                shown.getNeeds());
    }

    @Test
    public void plan_nullPlan_failsWithAClearMessage() {
        PresetPlanner planner = new PresetPlanner().answerNull();

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> RuleRunner.plan(ALL, planner, TODAY, THRESHOLDS));

        assertTrue(error.getMessage().contains("RotationPlanner.plan returned null"));
    }

    // ----- Review scenarios (2026-10-11): the app's side only, with preset answers -----

    @Test
    public void plan_partialSwapOfSixAndTwo_showsTheSwapOfTwo_andTheNeedOfFour() {
        BatchInfo kitSix = batch(50, 0, ItemCategory.FOOD, 6, LocalDate.of(2026, 10, 9),
                LocationType.EMERGENCY_KIT, FoodClass.SHELF_STABLE_FOOD);
        BatchInfo pantryTwo = batch(51, 0, ItemCategory.FOOD, 2, LocalDate.of(2027, 6, 1),
                LocationType.PANTRY, FoodClass.SHELF_STABLE_FOOD);
        // The planner's answer is set up here. The app never works out 6 - 2 by itself.
        PresetPlanner planner = new PresetPlanner()
                .proposal(new SwapProposal(50, 51, 2))
                .note(new PlanNote(50, PlanReason.PARTLY_REPLACED, 4));

        RuleRunner.ShownPlan shown = RuleRunner.plan(Arrays.asList(kitSix, pantryTwo), planner,
                TODAY, THRESHOLDS);

        assertEquals(Collections.singletonList(new RuleRunner.ShownSwap(kitSix, pantryTwo, 2)),
                shown.getSwaps());
        assertEquals(Collections.singletonList(
                new RuleRunner.ShownNeed(kitSix, PlanReason.PARTLY_REPLACED, 4)), shown.getNeeds());
    }

    @Test
    public void plan_dropsALaterProposalThatWouldTakeTheSameKitUnitsAgain() {
        BatchInfo secondPantry = batch(52, 0, ItemCategory.FOOD, 5, LocalDate.of(2027, 7, 1),
                LocationType.PANTRY, null);
        PresetPlanner planner = new PresetPlanner()
                .proposal(new SwapProposal(1, 2, 3))    // 3 of the kit batch's 5
                .proposal(new SwapProposal(1, 52, 3))   // 3 more would be 6 of 5: dropped
                .proposal(new SwapProposal(1, 52, 2));  // 2 more are exactly the 5: kept

        RuleRunner.ShownPlan shown = RuleRunner.plan(
                Arrays.asList(BARS_KIT, BARS_PANTRY, secondPantry), planner, TODAY, THRESHOLDS);

        assertEquals(Arrays.asList(new RuleRunner.ShownSwap(BARS_KIT, BARS_PANTRY, 3),
                new RuleRunner.ShownSwap(BARS_KIT, secondPantry, 2)), shown.getSwaps());
    }

    @Test
    public void plan_dropsAProposalWithMedicineOnThePantrySide() {
        // The two pantry batches differ only in their category.
        BatchInfo pantryMedicine = batch(53, 0, ItemCategory.MEDICINE, 5, LocalDate.of(2027, 6, 1),
                LocationType.PANTRY, null);
        BatchInfo pantryFood = batch(54, 0, ItemCategory.FOOD, 5, LocalDate.of(2027, 6, 1),
                LocationType.PANTRY, null);
        PresetPlanner planner = new PresetPlanner()
                .proposal(new SwapProposal(1, 53, 1))
                .proposal(new SwapProposal(1, 54, 1));

        RuleRunner.ShownPlan shown = RuleRunner.plan(
                Arrays.asList(BARS_KIT, pantryMedicine, pantryFood), planner, TODAY, THRESHOLDS);

        assertEquals(Collections.singletonList(new RuleRunner.ShownSwap(BARS_KIT, pantryFood, 1)),
                shown.getSwaps());
    }

    @Test
    public void statusesAndPlans_passEveryLabelType_printedText_confirmation_andConditionAnswers() {
        List<BatchInfo> batches = Arrays.asList(
                detailed(60, LocalDate.of(2026, 11, 1), null, DateLabel.USE_BY, true, null, null, null),
                detailed(61, LocalDate.of(2026, 11, 2), null, DateLabel.EXPIRES, true, null, null, null),
                detailed(62, LocalDate.of(2026, 11, 3), null, DateLabel.OTHER, true, null, null, null),
                detailed(63, null, "03/2027", DateLabel.BEST_BY, false, null, null, null),
                detailed(64, LocalDate.of(2026, 11, 4), null, DateLabel.BEST_BY, false, null, null, null),
                detailed(65, LocalDate.of(2026, 11, 5), null, DateLabel.BEST_BY, true,
                        ConditionAnswer.NO, ConditionAnswer.YES, ConditionAnswer.YES));
        RecordingPolicy policy = new RecordingPolicy(DateStatus.LATER);
        PresetPlanner planner = new PresetPlanner();

        RuleRunner.withStatuses(batches, policy, TODAY, THRESHOLDS);
        RuleRunner.plan(batches, planner, TODAY, THRESHOLDS);

        assertEquals(DateLabel.USE_BY, policy.inputs.get(0).getLabel());
        assertEquals(DateLabel.EXPIRES, policy.inputs.get(1).getLabel());
        assertEquals(DateLabel.OTHER, policy.inputs.get(2).getLabel());
        assertNull(policy.inputs.get(3).getDate());
        assertEquals("03/2027", policy.inputs.get(3).getPrintedText());
        assertFalse(policy.inputs.get(3).isConfirmed());
        assertEquals(LocalDate.of(2026, 11, 4), policy.inputs.get(4).getDate());
        assertFalse(policy.inputs.get(4).isConfirmed());
        // The planner gets the same date inputs and the household's answers, unchanged.
        for (int i = 0; i < batches.size(); i++) {
            assertEquals(policy.inputs.get(i), planner.received.get(i).getDate());
        }
        assertEquals(ConditionAnswer.NO, planner.received.get(5).getSealed());
        assertEquals(ConditionAnswer.YES, planner.received.get(5).getDamaged());
        assertEquals(ConditionAnswer.YES, planner.received.get(5).getStorageConcern());
        assertNull(planner.received.get(0).getDamaged()); // "Not checked"
    }

    // ----- Test helpers -----

    /** A real food batch in a kit, with a chosen date entry and condition answers. */
    private static BatchInfo detailed(long id, LocalDate date, String printedText, DateLabel label,
                                      boolean confirmed, ConditionAnswer sealed,
                                      ConditionAnswer damaged, ConditionAnswer storageConcern) {
        return new BatchInfo(id, 0, "Item " + id, "", "", "units", null, ItemCategory.FOOD, 5,
                date, printedText, label, confirmed, 2, "Earthquake kit", LocationType.EMERGENCY_KIT,
                null, sealed, null, damaged, storageConcern, false, false);
    }

    private static BatchInfo batch(long id, ItemCategory category, LocalDate date,
                                   LocationType locationType, boolean sample) {
        String location = locationType == LocationType.PANTRY ? "Pantry" : "Earthquake kit";
        return new BatchInfo(id, "Item " + id, category, 5, "units", date, DateLabel.BEST_BY,
                locationType == LocationType.PANTRY ? 1 : 2, location, locationType, sample);
    }

    /** A real (not sample) batch with a chosen product, quantity, place, and food class. */
    private static BatchInfo batch(long id, long productId, ItemCategory category, int quantity,
                                   LocalDate date, LocationType locationType, FoodClass foodClass) {
        long locationId = locationType.ordinal() + 1;
        return new BatchInfo(id, productId, "Item " + id, "", "", "units", foodClass, category,
                quantity, date, null, DateLabel.BEST_BY, date != null, locationId,
                locationType.name(), locationType, null, null, null, null, null, false, false);
    }

    /** Returns the same status for every input, and remembers what it was given. Not a DatePolicy. */
    private static final class RecordingPolicy implements DatePolicyContract {
        private final DateStatus answer;
        private final List<DateInput> inputs = new ArrayList<>();
        private final List<LocalDate> days = new ArrayList<>();
        private final List<Integer> nearDateDays = new ArrayList<>();

        RecordingPolicy(DateStatus answer) {
            this.answer = answer;
        }

        @Override
        public DateStatus statusOf(DateInput date, LocalDate today, int nearDays) {
            inputs.add(date);
            days.add(today);
            nearDateDays.add(nearDays);
            return answer;
        }
    }

    /** Returns the status set up for each batch's date input. Not a DatePolicy. */
    private static final class PresetPolicy implements DatePolicyContract {
        private final Map<DateInput, DateStatus> statuses = new HashMap<>();

        PresetPolicy status(BatchInfo batch, DateStatus status) {
            statuses.put(DateInput.from(batch), status);
            return this;
        }

        @Override
        public DateStatus statusOf(DateInput date, LocalDate today, int nearDateDays) {
            return statuses.get(date);
        }
    }

    /** Returns the plan set up by each test, and remembers what it was given. Not a RotationPlanner. */
    private static final class PresetPlanner implements RotationPlannerContract {
        private final List<SwapProposal> proposals = new ArrayList<>();
        private final List<PlanNote> notes = new ArrayList<>();
        private boolean answerNull;
        private List<PlanBatch> received;
        private LocalDate today;
        private int nearDateDays;
        private int replacementDays;

        PresetPlanner proposal(SwapProposal proposal) {
            proposals.add(proposal);
            return this;
        }

        PresetPlanner note(PlanNote note) {
            notes.add(note);
            return this;
        }

        PresetPlanner answerNull() {
            answerNull = true;
            return this;
        }

        @Override
        public RotationPlan plan(List<PlanBatch> batches, LocalDate day, int nearDays,
                                 int replacementDayCount) {
            received = batches;
            today = day;
            nearDateDays = nearDays;
            replacementDays = replacementDayCount;
            return answerNull ? null : new RotationPlan(proposals, notes);
        }
    }
}
