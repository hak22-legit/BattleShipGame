package com.battleship.view.quiz;

/**
 * one multiple-choice question shown by the nuclearlaunchdialog "mysterious box"
 * before a nuclear launcher shot is allowed to fire. pure ui data — not a domain
 * model, so it lives under view rather than model.
 */
public record QuizQuestion(String prompt, String[] options, int correctIndex) {
}
