package com.example.readyshelf.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The main rule tests for RotationPlanner: the swap plan for the batches of one household
 * (SPEC.md, "RotationPlanner requirements" and its decisions RP1 to RP9).
 *
 * <p>Claude Code wrote these tests on 2026-10-18 at the student's request (CLAUDE.md rules 1 and
 * 2). The student approved every case before RotationPlanner was written (AI-USE.md). The
 * section letters (A to G) are the ones in the approved plan. On 2026-10-22 the student chose
 * swaps for every category but medicine, documents, and subscriptions, with no food class or
 * condition checks, and approved the changed cases in section D (AI-USE.md).
 *
 * <p>Every test passes a fixed "today", so no answer depends on the phone's clock. Unless a test
 * says otherwise, a batch is 6 bars of product 1: Food, a confirmed Best by date, no food class,
 * and no condition answers ("Not checked"), like every batch saved since 2026-10-22. Such a batch
 * meets every swap rule, so each test changes only what it is about.
 */
public class RotationPlannerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 18);
    private static final int NEAR_DAYS = 30;          // the Settings default
    private static final int REPLACEMENT_DAYS = 180;  // the Settings default

    private static final List<SwapProposal> NO_SWAPS = Collections.emptyList();
    /** The categories that are never swapped (the student's choice on 2026-10-22). */
    private static final ItemCategory[] NEVER_SWAPPED = {ItemCategory.MEDICINE,
            ItemCategory.DOCUMENT, ItemCategory.SUBSCRIPTION};

    private final RotationPlannerContract planner = new RotationPlanner();

    // ----- A. Swaps and amounts -----

    @Test
    public void nearKitBatch_andALaterPantryBatch_swapAllOfIt() {
        RotationPlan plan = plan(kit(1, 20), pantry(2, 240).quantity(10));

        assertEquals(Arrays.asList(swap(1, 2, 6)), plan.getProposals());
        assertEquals(Arrays.asList(note(1, PlanReason.SWAP_PROPOSED, 0)), plan.getNotes());
    }

    @Test
    public void sixInTheKit_twoInThePantry_swapTwo_andFourStillNeedReplacement() {
        RotationPlan plan = plan(kit(1, 20), pantry(2, 240).quantity(2));

        assertEquals(Arrays.asList(swap(1, 2, 2)), plan.getProposals());
        assertEquals(Arrays.asList(note(1, PlanReason.PARTLY_REPLACED, 4)), plan.getNotes());
    }

    @Test
    public void noPantryBatchOfTheProduct_noSwap_andAllStillNeedReplacement() {
        RotationPlan plan = plan(kit(1, 14).quantity(4));

        assertEquals(NO_SWAPS, plan.getProposals());
        assertEquals(Arrays.asList(note(1, PlanReason.NO_REPLACEMENT, 4)), plan.getNotes());
    }

    @Test
    public void oneKitBatch_usesTwoPantryBatches_earliestQualifyingDateFirst() {
        RotationPlan plan = plan(kit(1, 20), pantry(2, 300).quantity(4), pantry(3, 200).quantity(5));

        assertEquals(Arrays.asList(swap(1, 3, 5), swap(1, 2, 1)), plan.getProposals());
        assertEquals(Arrays.asList(note(1, PlanReason.SWAP_PROPOSED, 0)), plan.getNotes());
    }

    @Test
    public void twoKitBatches_shareAPantryBatchOfThree_earliestKitDateFirst() {
        RotationPlan plan = plan(kit(1, 10).quantity(2), kit(2, 5).quantity(2),
                pantry(3, 240).quantity(3));

        // Each pantry unit goes to one proposal at most: 2 + 1 are the 3 units of batch 3.
        assertEquals(Arrays.asList(swap(2, 3, 2), swap(1, 3, 1)), plan.getProposals());
        assertEquals(Arrays.asList(note(1, PlanReason.PARTLY_REPLACED, 1),
                note(2, PlanReason.SWAP_PROPOSED, 0)), plan.getNotes());
    }

    @Test
    public void kitBatchesWithTheSameDate_lowerBatchIdFirst() {
        RotationPlan plan = plan(kit(5, 10).quantity(2), kit(4, 10).quantity(2),
                pantry(9, 240).quantity(3));

        assertEquals(Arrays.asList(swap(4, 9, 2), swap(5, 9, 1)), plan.getProposals());
        assertEquals(Arrays.asList(note(4, PlanReason.SWAP_PROPOSED, 0),
                note(5, PlanReason.PARTLY_REPLACED, 1)), plan.getNotes());
    }

    @Test
    public void pantryBatchesWithTheSameDate_lowerBatchIdFirst() {
        RotationPlan plan = plan(kit(1, 10).quantity(2), pantry(8, 200).quantity(5),
                pantry(7, 200).quantity(5));

        assertEquals(Arrays.asList(swap(1, 7, 2)), plan.getProposals());
        assertEquals(Arrays.asList(note(1, PlanReason.SWAP_PROPOSED, 0)), plan.getNotes());
    }

    @Test
    public void anotherProduct_isNeverAReplacement() {
        // Another variant or package size is another product, with another product id.
        RotationPlan plan = plan(kit(1, 20), pantry(2, 240).product(2));

        assertEquals(NO_SWAPS, plan.getProposals());
        assertEquals(Arrays.asList(note(1, PlanReason.NO_REPLACEMENT, 6)), plan.getNotes());
    }

    @Test
    public void sameProductIdWithAnotherUnit_isNeverAReplacement() {
        RotationPlan plan = plan(kit(1, 20), pantry(2, 240).unit("boxes"));

        assertEquals(NO_SWAPS, plan.getProposals());
        assertEquals(Arrays.asList(note(1, PlanReason.NO_REPLACEMENT, 6)), plan.getNotes());
    }

    // ----- B. Kit dates and the Settings numbers -----

    @Test
    public void kitDateTodayOrPassed_isNeverSwapped_andAllOfItNeedsReplacement() {
        // The guide: "Never recommend pantry use for a source dated today or earlier."
        RotationPlan plan = plan(kit(1, 0), kit(2, -1), pantry(3, 240).quantity(20));

        assertEquals(NO_SWAPS, plan.getProposals());
        assertEquals(Arrays.asList(note(1, PlanReason.DATE_TODAY_OR_PASSED, 6),
                note(2, PlanReason.DATE_TODAY_OR_PASSED, 6)), plan.getNotes());
    }

    @Test
    public void kitDate1To30DaysAway_isSwapped_and31_isNotNearYet() {
        RotationPlan plan = plan(kit(1, 1), kit(2, 30), kit(3, 31), pantry(4, 240).quantity(20));

        assertEquals(Arrays.asList(swap(1, 4, 6), swap(2, 4, 6)), plan.getProposals());
        assertEquals(Arrays.asList(note(1, PlanReason.SWAP_PROPOSED, 0),
                note(2, PlanReason.SWAP_PROPOSED, 0),
                note(3, PlanReason.NOT_NEAR_DATE, 0)), plan.getNotes());
    }

    @Test
    public void theSettingsNumbers_areUsed_notFixedNumbers() {
        TestBatch[] batches = {kit(1, 10).quantity(2), kit(2, 20).quantity(2),
                pantry(3, 100).quantity(10)};

        // 30 and 180: both kit batches are near, but 100 days is too soon for a replacement.
        RotationPlan defaults = planWithSettings(30, 180, batches);
        assertEquals(NO_SWAPS, defaults.getProposals());
        assertEquals(Arrays.asList(note(1, PlanReason.NO_REPLACEMENT, 2),
                note(2, PlanReason.NO_REPLACEMENT, 2)), defaults.getNotes());

        // 14 and 90: only the kit batch 10 days away is near, and 100 days is enough.
        RotationPlan changed = planWithSettings(14, 90, batches);
        assertEquals(Arrays.asList(swap(1, 3, 2)), changed.getProposals());
        assertEquals(Arrays.asList(note(1, PlanReason.SWAP_PROPOSED, 0),
                note(2, PlanReason.NOT_NEAR_DATE, 0)), changed.getNotes());
    }

    @Test
    public void zeroNearDateDays_meansNoKitDateIsNear() {
        // RP9, like DP5 for the date rules.
        RotationPlan plan = planWithSettings(0, 180, kit(1, 1), kit(2, 0), pantry(3, 240));

        assertEquals(NO_SWAPS, plan.getProposals());
        assertEquals(Arrays.asList(note(1, PlanReason.NOT_NEAR_DATE, 0),
                note(2, PlanReason.DATE_TODAY_OR_PASSED, 6)), plan.getNotes());
    }

    @Test
    public void negativeSettingsNumbers_areAnError() {
        // RP9: Settings never allow them, so they can only be a mistake in the calling code.
        List<PlanBatch> batches = batches(kit(1, 20), pantry(2, 240));

        assertThrows(IllegalArgumentException.class,
                () -> planner.plan(batches, TODAY, -1, REPLACEMENT_DAYS));
        assertThrows(IllegalArgumentException.class,
                () -> planner.plan(batches, TODAY, NEAR_DAYS, -1));
    }

    // ----- C. Pantry dates -----

    @Test
    public void pantryDate180DaysAway_qualifies_and179_doesNot() {
        RotationPlan plan = plan(kit(1, 20), pantry(2, 179), pantry(3, 180));

        assertEquals(Arrays.asList(swap(1, 3, 6)), plan.getProposals());
        assertEquals(Arrays.asList(note(1, PlanReason.SWAP_PROPOSED, 0)), plan.getNotes());
    }

    @Test
    public void pantryDate_mustBeLaterThanTheKitDate() {
        // Settings would refuse 30 and 10 (RP9). With them, only "later than the kit batch's
        // date" keeps out the pantry batches 20 and 25 days away: the same day isn't later (RP8).
        RotationPlan plan = planWithSettings(30, 10,
                kit(1, 25), pantry(2, 20), pantry(3, 25), pantry(4, 26));

        assertEquals(Arrays.asList(swap(1, 4, 6)), plan.getProposals());
        assertEquals(Arrays.asList(note(1, PlanReason.SWAP_PROPOSED, 0)), plan.getNotes());
    }

    @Test
    public void replacementDays_areCalendarDays_acrossALeapDay() {
        // From 18 Oct 2027, the 180 calendar days (29 Feb 2028 is one of them) end on 15 Apr 2028.
        LocalDate today = LocalDate.of(2027, 10, 18);
        List<PlanBatch> batches = batches(
                kit(1, LocalDate.of(2027, 10, 28)),
                pantry(2, LocalDate.of(2028, 4, 14)),   // 179 days away
                pantry(3, LocalDate.of(2028, 4, 15)));  // 180 days away

        RotationPlan plan = planner.plan(batches, today, NEAR_DAYS, REPLACEMENT_DAYS);

        assertEquals(Arrays.asList(swap(1, 3, 6)), plan.getProposals());
    }

    // ----- D. What is never swapped, and what no longer matters (changed 2026-10-22) -----
    // On the kit side, the note gives the reason. On the pantry side, kit batch 1 has no
    // replacement. Each case has a batch on the other side that would otherwise qualify.

    @Test
    public void medicineDocumentsAndSubscriptionsInAKit_areNeverSwapped() {
        for (ItemCategory category : NEVER_SWAPPED) {
            assertNoSwap(kit(1, 10).category(category), pantry(2, 240).category(category),
                    note(1, PlanReason.CATEGORY_NOT_SWAPPED, 0));
        }
    }

    @Test
    public void aPantryBatchOfMedicineADocumentOrASubscription_isNeverAReplacement() {
        for (ItemCategory category : NEVER_SWAPPED) {
            assertNoSwap(kit(1, 10), pantry(2, 240).category(category),
                    note(1, PlanReason.NO_REPLACEMENT, 6));
        }
    }

    @Test
    public void everyOtherCategory_isSwapped() {
        // Food, water, first aid supplies, cosmetics, and Other. A category added to ItemCategory
        // later fails here until the student decides whether it is swapped.
        List<ItemCategory> neverSwapped = Arrays.asList(NEVER_SWAPPED);
        for (ItemCategory category : ItemCategory.values()) {
            if (!neverSwapped.contains(category)) {
                assertSwapOfSix(kit(1, 20).category(category), pantry(2, 240).category(category));
            }
        }
    }

    @Test
    public void theFoodClassAndTheConditionAnswers_ofAKitBatch_doNotMatter() {
        // Until 2026-10-22 each of these kept the kit batch out of swaps. ("Not classified" and
        // "Not checked" are the defaults, so every other test covers them.)
        TestBatch[] kits = {
                kit(1, 10).foodClass(FoodClass.INFANT_FORMULA),
                kit(1, 10).foodClass(FoodClass.PERISHABLE_FOOD),
                kit(1, 10).foodClass(FoodClass.SHELF_STABLE_FOOD),
                kit(1, 10).sealed(ConditionAnswer.NO),
                kit(1, 10).damaged(ConditionAnswer.YES),
                kit(1, 10).storageConcern(ConditionAnswer.YES),
                kit(1, 10).foodClass(FoodClass.PERISHABLE_FOOD).sealed(ConditionAnswer.NO)
                        .damaged(ConditionAnswer.YES).storageConcern(ConditionAnswer.YES)};
        for (TestBatch kit : kits) {
            assertSwapOfSix(kit, pantry(2, 240));
        }
    }

    @Test
    public void theFoodClassAndTheConditionAnswers_ofAPantryBatch_doNotMatter() {
        TestBatch[] pantries = {
                pantry(2, 240).foodClass(FoodClass.INFANT_FORMULA),
                pantry(2, 240).foodClass(FoodClass.PERISHABLE_FOOD),
                pantry(2, 240).foodClass(FoodClass.SHELF_STABLE_FOOD),
                pantry(2, 240).sealed(ConditionAnswer.NO),
                pantry(2, 240).damaged(ConditionAnswer.YES),
                pantry(2, 240).storageConcern(ConditionAnswer.YES),
                pantry(2, 240).foodClass(FoodClass.PERISHABLE_FOOD).sealed(ConditionAnswer.NO)
                        .damaged(ConditionAnswer.YES).storageConcern(ConditionAnswer.YES)};
        for (TestBatch pantry : pantries) {
            assertSwapOfSix(kit(1, 10), pantry);
        }
    }

    @Test
    public void aKitBatchAndAPantryBatchOfOneProduct_swap_evenInDifferentCategories() {
        // The category belongs to each batch. Only medicine, documents, and subscriptions
        // (on either side) keep a swap out; the categories need not match.
        assertSwapOfSix(kit(1, 20).category(ItemCategory.WATER),
                pantry(2, 240).category(ItemCategory.OTHER));
    }

    @Test
    public void kitDateMissing_partial_notConfirmed_orOther_isNeverSwapped_andNeedsNothingYet() {
        // RP4: the date has to be reviewed first, so nothing is needed yet, even when the
        // recorded date has passed. (The app's own check still lists a batch of a category the
        // household buys again, dated today or earlier, on the Purchase list.)
        assertNoSwap(kit(1, 10).noDate(), pantry(2, 240),
                note(1, PlanReason.DATE_MISSING_OR_INCOMPLETE, 0));
        assertNoSwap(kit(1, 10).printed("03/2027"), pantry(2, 240),
                note(1, PlanReason.DATE_MISSING_OR_INCOMPLETE, 0));
        assertNoSwap(kit(1, 10).notConfirmed(), pantry(2, 240),
                note(1, PlanReason.DATE_NOT_CONFIRMED, 0));
        assertNoSwap(kit(1, -1).notConfirmed(), pantry(2, 240),
                note(1, PlanReason.DATE_NOT_CONFIRMED, 0));
        assertNoSwap(kit(1, 10).label(DateLabel.OTHER), pantry(2, 240),
                note(1, PlanReason.LABEL_TYPE_NOT_KNOWN, 0));
    }

    @Test
    public void pantryDateMissing_partial_notConfirmed_orOther_isNeverAReplacement() {
        TestBatch[] pantries = {
                pantry(2, 240).noDate(),
                pantry(2, 240).printed("07/2027"),
                pantry(2, 240).notConfirmed(),
                pantry(2, 240).label(DateLabel.OTHER)};
        for (TestBatch pantry : pantries) {
            assertNoSwap(kit(1, 10), pantry, note(1, PlanReason.NO_REPLACEMENT, 6));
        }
    }

    @Test
    public void bestBy_useBy_andExpires_areKnownLabelTypes_andNeedNotMatch() {
        DateLabel[] known = {DateLabel.BEST_BY, DateLabel.USE_BY, DateLabel.EXPIRES};
        for (DateLabel kitLabel : known) {
            for (DateLabel pantryLabel : known) {
                RotationPlan plan = plan(kit(1, 20).label(kitLabel),
                        pantry(2, 240).label(pantryLabel));

                assertEquals(kitLabel + " / " + pantryLabel, Arrays.asList(swap(1, 2, 6)),
                        plan.getProposals());
            }
        }
    }

    @Test
    public void aKitBatchFailingSeveralChecks_getsTheFirstReason() {
        // RP5 (since 2026-10-22): the category, then a date the rules can use, then today or
        // earlier, then near.
        assertNoSwap(kit(1, 10).category(ItemCategory.MEDICINE).noDate(), pantry(2, 240),
                note(1, PlanReason.CATEGORY_NOT_SWAPPED, 0));
        assertNoSwap(kit(1, -1).category(ItemCategory.SUBSCRIPTION), pantry(2, 240),
                note(1, PlanReason.CATEGORY_NOT_SWAPPED, 0));
        assertNoSwap(kit(1, 10).category(ItemCategory.WATER).noDate(), pantry(2, 240),
                note(1, PlanReason.DATE_MISSING_OR_INCOMPLETE, 0));
        assertNoSwap(kit(1, -1).damaged(ConditionAnswer.YES), pantry(2, 240),
                note(1, PlanReason.DATE_TODAY_OR_PASSED, 6));
        assertNoSwap(kit(1, 60).foodClass(FoodClass.PERISHABLE_FOOD), pantry(2, 240),
                note(1, PlanReason.NOT_NEAR_DATE, 0));
    }

    // ----- E. Places -----

    @Test
    public void foodInAFirstAidLocation_isKitFood_too() {
        RotationPlan plan = plan(batchIn(LocationType.FIRST_AID, 1, 10), pantry(2, 240));

        assertEquals(Arrays.asList(swap(1, 2, 6)), plan.getProposals());
        assertEquals(Arrays.asList(note(1, PlanReason.SWAP_PROPOSED, 0)), plan.getNotes());
    }

    @Test
    public void onlyPantryBatchesReplace_andOnlyKitBatchesGetNotes() {
        RotationPlan plan = plan(
                kit(1, 10),
                batchIn(LocationType.FIRST_AID, 2, 240),  // a kit batch whose date isn't near
                batchIn(LocationType.OTHER, 3, 240),
                pantry(4, 5),                             // near its date, but in the pantry
                batchIn(LocationType.OTHER, 5, 5));

        assertEquals(NO_SWAPS, plan.getProposals());
        assertEquals(Arrays.asList(note(1, PlanReason.NO_REPLACEMENT, 6),
                note(2, PlanReason.NOT_NEAR_DATE, 0)), plan.getNotes());
    }

    // ----- F. The list the app passes in, and the plan -----

    @Test
    public void batchesWithNoUnits_areLeftOut() {
        RotationPlan plan = plan(kit(1, 10).quantity(0), kit(2, 10), pantry(3, 240).quantity(0));

        assertEquals(NO_SWAPS, plan.getProposals());
        assertEquals(Arrays.asList(note(2, PlanReason.NO_REPLACEMENT, 6)), plan.getNotes());
    }

    @Test
    public void theGivenList_isNotChanged_andNothingIsKeptBetweenPlans() {
        List<PlanBatch> given = batches(kit(1, 20), kit(2, 5).quantity(2),
                pantry(3, 240).quantity(4));
        List<PlanBatch> copy = new ArrayList<>(given);

        // The app passes a list that can't be changed, so sorting it in place would fail here.
        RotationPlan first = planner.plan(Collections.unmodifiableList(given), TODAY, NEAR_DAYS,
                REPLACEMENT_DAYS);
        RotationPlan second = planner.plan(Collections.unmodifiableList(given), TODAY, NEAR_DAYS,
                REPLACEMENT_DAYS);

        assertEquals(copy, given);
        assertEquals(first, second);
    }

    @Test
    public void theOrderOfTheGivenBatches_doesNotChangeThePlan() {
        List<PlanBatch> forwards = batches(
                kit(1, 10).quantity(2), kit(2, 5).quantity(2), pantry(3, 240).quantity(3),
                kit(4, 10).product(2).quantity(2), pantry(5, 200).product(2),
                pantry(6, 200).product(2),
                batchIn(LocationType.FIRST_AID, 7, -10).category(ItemCategory.MEDICINE));
        List<PlanBatch> backwards = new ArrayList<>(forwards);
        Collections.reverse(backwards);

        assertEquals(planner.plan(forwards, TODAY, NEAR_DAYS, REPLACEMENT_DAYS),
                planner.plan(backwards, TODAY, NEAR_DAYS, REPLACEMENT_DAYS));
    }

    @Test
    public void noBatches_giveAnEmptyPlan() {
        RotationPlan plan = planner.plan(Collections.emptyList(), TODAY, NEAR_DAYS,
                REPLACEMENT_DAYS);

        assertNotNull(plan);
        assertEquals(NO_SWAPS, plan.getProposals());
        assertEquals(Collections.emptyList(), plan.getNotes());
    }

    // ----- G. The demo (DEMO.md, "The sample data") -----

    @Test
    public void demoSampleData_givesTheSwapAndTheNeedTheDemoShows() {
        RotationPlan plan = plan(
                kit(1, 20),                                         // granola bars, Earthquake kit
                pantry(2, 240),                                     // granola bars, Pantry
                pantry(3, 270).printed("07/2027").notConfirmed().quantity(3),
                kit(4, 14).product(2).unit("cans").quantity(4),     // canned beans, none in the pantry
                batchIn(LocationType.FIRST_AID, 5, -10).product(3).unit("bottles").quantity(2)
                        .category(ItemCategory.MEDICINE).label(DateLabel.EXPIRES));  // pain reliever

        assertEquals(Arrays.asList(swap(1, 2, 6)), plan.getProposals());
        assertEquals(Arrays.asList(note(1, PlanReason.SWAP_PROPOSED, 0),
                note(4, PlanReason.NO_REPLACEMENT, 4),
                note(5, PlanReason.CATEGORY_NOT_SWAPPED, 0)), plan.getNotes());
    }

    // ----- Helpers -----

    /** The plan for these batches, with the fixed today and the Settings defaults (30 and 180). */
    private RotationPlan plan(TestBatch... batches) {
        return planWithSettings(NEAR_DAYS, REPLACEMENT_DAYS, batches);
    }

    private RotationPlan planWithSettings(int nearDateDays, int replacementDays,
                                          TestBatch... batches) {
        return planner.plan(batches(batches), TODAY, nearDateDays, replacementDays);
    }

    /**
     * Plans with a kit batch (id 1) and a pantry batch (id 2). Checks that nothing is swapped,
     * and that the kit batch gets the expected note.
     */
    private void assertNoSwap(TestBatch kit, TestBatch pantry, PlanNote expectedKitNote) {
        RotationPlan plan = plan(kit, pantry);
        String batches = kit + " | " + pantry;

        assertEquals(batches, NO_SWAPS, plan.getProposals());
        assertEquals(batches, Arrays.asList(expectedKitNote), plan.getNotes());
    }

    /**
     * Plans with a kit batch (id 1) and a pantry batch (id 2) of 6 units each. Checks that all 6
     * are swapped.
     */
    private void assertSwapOfSix(TestBatch kit, TestBatch pantry) {
        RotationPlan plan = plan(kit, pantry);
        String batches = kit + " | " + pantry;

        assertEquals(batches, Arrays.asList(swap(1, 2, 6)), plan.getProposals());
        assertEquals(batches, Arrays.asList(note(1, PlanReason.SWAP_PROPOSED, 0)), plan.getNotes());
    }

    private static List<PlanBatch> batches(TestBatch... batches) {
        List<PlanBatch> result = new ArrayList<>();
        for (TestBatch batch : batches) {
            result.add(batch.build());
        }
        return result;
    }

    private static SwapProposal swap(long kitBatchId, long pantryBatchId, int quantity) {
        return new SwapProposal(kitBatchId, pantryBatchId, quantity);
    }

    private static PlanNote note(long batchId, PlanReason reason, int stillNeeded) {
        return new PlanNote(batchId, reason, stillNeeded);
    }

    /** A batch in an Emergency kit, dated {@code daysAway} days after today. */
    private static TestBatch kit(long id, int daysAway) {
        return batchIn(LocationType.EMERGENCY_KIT, id, daysAway);
    }

    private static TestBatch kit(long id, LocalDate date) {
        return new TestBatch(id, LocationType.EMERGENCY_KIT, date);
    }

    /** A batch in a Pantry location, dated {@code daysAway} days after today. */
    private static TestBatch pantry(long id, int daysAway) {
        return batchIn(LocationType.PANTRY, id, daysAway);
    }

    private static TestBatch pantry(long id, LocalDate date) {
        return new TestBatch(id, LocationType.PANTRY, date);
    }

    private static TestBatch batchIn(LocationType place, long id, int daysAway) {
        return new TestBatch(id, place, TODAY.plusDays(daysAway));
    }

    /**
     * Makes one PlanBatch for a test. It starts as 6 bars of product 1 that meet every swap
     * rule (Food, a confirmed Best by date, no food class, and no condition answers), and each
     * method changes one thing and returns the same TestBatch, so the changes can be chained:
     * {@code pantry(2, 240).quantity(10)}. Only the test helpers call {@link #build()}.
     */
    private static final class TestBatch {
        private final long id;
        private final LocationType place;
        private long productId = 1;
        private String unit = "bars";
        private ItemCategory category = ItemCategory.FOOD;
        private FoodClass foodClass = null;        // "Not classified", like every new product
        private int quantity = 6;
        private LocalDate date;
        private String printedText = null;
        private DateLabel label = DateLabel.BEST_BY;
        private boolean confirmed = true;
        private ConditionAnswer sealed = null;     // "Not checked", like every new batch
        private ConditionAnswer damaged = null;
        private ConditionAnswer storageConcern = null;

        TestBatch(long id, LocationType place, LocalDate date) {
            this.id = id;
            this.place = place;
            this.date = date;
        }

        TestBatch quantity(int units) {
            quantity = units;
            return this;
        }

        TestBatch product(long newProductId) {
            productId = newProductId;
            return this;
        }

        TestBatch unit(String text) {
            unit = text;
            return this;
        }

        TestBatch category(ItemCategory newCategory) {
            category = newCategory;
            return this;
        }

        /** null means the product has no food class yet. */
        TestBatch foodClass(FoodClass newClass) {
            foodClass = newClass;
            return this;
        }

        /** No complete date and no printed text. */
        TestBatch noDate() {
            date = null;
            printedText = null;
            return this;
        }

        /** Only part of a date was recorded: no complete date, only this text. */
        TestBatch printed(String text) {
            date = null;
            printedText = text;
            return this;
        }

        TestBatch label(DateLabel newLabel) {
            label = newLabel;
            return this;
        }

        TestBatch notConfirmed() {
            confirmed = false;
            return this;
        }

        /** null means "Not checked". */
        TestBatch sealed(ConditionAnswer answer) {
            sealed = answer;
            return this;
        }

        /** null means "Not checked". */
        TestBatch damaged(ConditionAnswer answer) {
            damaged = answer;
            return this;
        }

        /** null means "Not checked". */
        TestBatch storageConcern(ConditionAnswer answer) {
            storageConcern = answer;
            return this;
        }

        PlanBatch build() {
            return new PlanBatch(id, productId, unit, category, foodClass, place, quantity,
                    new DateInput(date, printedText, label, confirmed), sealed, damaged,
                    storageConcern);
        }

        /** Every field, so a failing case in a loop says which batch it was. */
        @Override
        public String toString() {
            return "batch " + id + " in " + place + ": " + quantity + " " + unit + " of product "
                    + productId + ", " + category + ", " + foodClass + ", date " + date
                    + ", printed " + printedText + ", " + label
                    + (confirmed ? ", confirmed" : ", not confirmed") + ", sealed " + sealed
                    + ", damaged " + damaged + ", storage concern " + storageConcern;
        }
    }
}
