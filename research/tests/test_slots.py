from features.slots import HandCandidate, assign_slots


def candidate(label, wrist, index):
    return HandCandidate(handedness=label, wrist=wrist, index=index)


def test_distinct_labels_use_their_named_slots():
    result = assign_slots(
        [candidate("right", (0.8, 0.5), 0), candidate("left", (0.2, 0.5), 1)],
        previous=None,
    )
    assert result.slots["left"].index == 1
    assert result.slots["right"].index == 0
    assert result.ambiguous is False


def test_same_labels_are_ambiguous_and_fall_back_to_input_order():
    result = assign_slots(
        [candidate("left", (0.8, 0.5), 0), candidate("left", (0.2, 0.5), 1)],
        previous=None,
    )
    assert result.slots["left"].index == 0
    assert result.slots["right"].index == 1
    assert result.ambiguous is True


def test_continuity_beats_input_order_for_same_labels():
    result = assign_slots(
        [candidate("left", (0.8, 0.5), 0), candidate("left", (0.2, 0.5), 1)],
        previous={"left": (0.2, 0.5), "right": (0.8, 0.5)},
    )
    assert result.slots["left"].index == 1
    assert result.slots["right"].index == 0
    assert result.ambiguous is True


def test_continuity_tie_keeps_input_order():
    result = assign_slots(
        [candidate(None, (0.5, 0.5), 0), candidate(None, (0.5, 0.5), 1)],
        previous={"left": (0.5, 0.5), "right": (0.5, 0.5)},
    )
    assert result.slots["left"].index == 0
    assert result.slots["right"].index == 1


def test_single_known_hand_uses_its_label():
    result = assign_slots([candidate("right", (0.8, 0.5), 0)], previous=None)
    assert result.slots["right"].index == 0
    assert result.slots["left"] is None
    assert result.ambiguous is False


def test_single_unknown_hand_uses_the_only_previous_slot():
    result = assign_slots(
        [candidate(None, (0.8, 0.5), 0)],
        previous={"right": (0.8, 0.5)},
    )
    assert result.slots["right"].index == 0
    assert result.ambiguous is True


def test_single_unknown_hand_without_history_defaults_left():
    result = assign_slots([candidate(None, (0.8, 0.5), 0)], previous=None)
    assert result.slots["left"].index == 0
    assert result.slots["right"] is None
    assert result.ambiguous is True


def test_no_hands_is_not_ambiguous():
    result = assign_slots([], previous={"left": (0.2, 0.5)})
    assert result.slots == {"left": None, "right": None}
    assert result.ambiguous is False


def test_more_than_two_hands_is_rejected():
    import pytest

    with pytest.raises(ValueError):
        assign_slots(
            [
                candidate("left", (0.1, 0.1), 0),
                candidate("right", (0.9, 0.1), 1),
                candidate(None, (0.5, 0.5), 2),
            ],
            previous=None,
        )
