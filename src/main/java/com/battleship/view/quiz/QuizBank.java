package com.battleship.view.quiz;

import java.security.SecureRandom;
import java.util.List;

/**
 * fixed bank of khmer-history trivia questions used to gate the nuclear
 * launcher. this is flavor text for a board game weapon — not real launch
 * authorization — so questions are deliberately quick to answer.
 */
public final class QuizBank {

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final List<QuizQuestion> QUESTIONS = List.of(
            new QuizQuestion("តើអង្គរវត្តត្រូវបានសាងសង់នៅសតវត្សរ៍ទីប៉ុន្មាន?",
                    new String[]{"ទី៩", "ទី១៥", "ទី១២", "ទី១៨"}, 2),
            new QuizQuestion("តើព្រះបាទជ័យវរ្ម័នទី៧ គោរពសាសនាអ្វី?",
                    new String[]{"ព្រះពុទ្ធសាសនា", "ហិណ្ឌូសាសនា", "គ្រិស្តសាសនា", "ឥស្លាមសាសនា"}, 0),
            new QuizQuestion("តើកម្ពុជាទទួលបានឯករាជ្យនៅឆ្នាំណា?",
                    new String[]{"១៩៥៣", "១៩៧៥", "១៩៤៩", "១៩៦០"}, 0),
            new QuizQuestion("តើកម្ពុជាទទួលបានឯករាជ្យពីប្រទេសណា?",
                    new String[]{"អង់គ្លេស", "ជប៉ុន", "អាមេរិក", "បារាំង"}, 3),
            new QuizQuestion("តើអក្សរខ្មែរបានកែចាន់ឡើងមកពីអក្សរណា?",
                    new String[]{"អក្សរឡាតាំង", "អក្សរបល្លវៈ", "អក្សរចិន", "អក្សរអារ៉ាប់"}, 1),
            new QuizQuestion("តើប្រាសាទបាយ័នស្ថិតនៅខេត្តណា?",
                    new String[]{"ភ្នំពេញ", "កំពត", "សៀមរាប", "កំពង់ចាម"}, 2),
            new QuizQuestion("តើសម័យដ៏រុងរឿងបំផុតនៃប្រវត្តិសាស្ត្រខ្មែរគឺសម័យអ្វី?",
                    new String[]{"សម័យអង្គរ", "សម័យចតុមុខ", "សម័យលង្វែក", "សម័យនគរភ្នំ"}, 0),
            new QuizQuestion("តើព្រះបាទជ័យវរ្ម័នទី២ បានប្រកាសឯករាជ្យនៅឆ្នាំណា?",
                    new String[]{"ឆ្នាំ១១១៣", "ឆ្នាំ១៤៣១", "ឆ្នាំ១៨៦៣", "ឆ្នាំ៨០២"}, 3),
            new QuizQuestion("តើប្រាសាទព្រះវិហារស្ថិតនៅខេត្តណា?",
                    new String[]{"ខេត្តព្រះវិហារ", "ខេត្តសៀមរាប", "ខេត្តរតនគិរី", "ខេត្តពោធិ៍សាត់"}, 0),
            new QuizQuestion("តើទីក្រុងអង្គរធំត្រូវបានសាងសង់ដោយព្រះបាទណា?",
                    new String[]{"សូរ្យវរ្ម័នទី២", "ជ័យវរ្ម័នទី៧", "យសោវរ្ម័នទី១", "ឥន្ទ្រវរ្ម័នទី១"}, 1)
    );

    private QuizBank() { }

    public static QuizQuestion random() {
        return QUESTIONS.get(RANDOM.nextInt(QUESTIONS.size()));
    }
}
