package com.deitel.flagquiz;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.graphics.drawable.Drawable;
import android.app.DialogFragment;
import android.app.Fragment;
import android.os.Handler;
import android.util.Log;
import android.view.View.OnClickListener;
import android.view.ViewAnimationUtils;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import android.widget.ImageView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;



public class MainActivityFragment extends Fragment {


    //TAG used for logging error messages
    private static final String TAG = "FlagQuiz Activity";
    //Number of flags in the quiz
    private static final int FLAGS_IN_QUIZ = 10;

    //Flag file names
    //flagNameList holds the flag-image file names for the current
    //enabled geo region.
    private List<String> fileNameList;
    //countries in current quiz
    //quizCountriesList holds the flag file names for the countries used in the current quiz
    private List<String> quizCountriesList;
    //world regions in current quiz
    //regionsSet stores the geo regions that are enabled.
    private Set<String> regionsSet;


    //correct country for the current flag
    //correctAnswer holds flag file name for current flags correct answer
    private String correctAnswer;
    //number of guesses made
    //totalGuesses stores total # of correct/incorrect guesses so far.
    private int totalGuesses;
    //number of correct guesses so far, this will eventually be equal to
    //FLAGS_IN_QUIZ if user completes the quiz.
    private int correctAnswers;
    //number of rows displaying guess Buttons
    //guessRows is # of 2-button LinearLayouts displaying the flag answer choices
    private int guessRows;
    //random is used to randomize the quiz
    //random is a random-number generator used to randomly pick the flags to include
    //in the quiz & which button in the 2-button rows represents the corr answer.
    private SecureRandom random;


    //handler used to delay loading next flag
    //When the user selects a correct answer and quiz is not over, we used to the handler
    //to load the next flag after a short delay.
    private Handler handler;
    //shakeAnimation used for animation for incorrect guess
    private Animation shakeAnimation;

    //layout that contains the quiz
    private LinearLayout quizLinearLayout;
    //Textview that shows current question number
    private TextView questionNumberTextView;
    //Imageview that displays a flag
    private ImageView flagImageView;
    //Array holds rows of answer Buttons
    private LinearLayout[] guessLinearLayouts;
    //TextView that displays correct answer
    private TextView answerTextView;





    //configures the MainActivityFragment when its View is created
    //onCreateView inflates the GUI and initializes most of MAFragment's instance variables.
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        //Here we inflate MAFragment's GUI.
        //r.layout.fragment_main is the resource id indicating layout to inflate
        //container is the ViewGroup in which the fragment will be displayed
        //a boolean indicates whether or not the inflated GUI needs to be attached
        //to the Viewgroup container, it should be false as the system auto-attaches
        //a fragment to the host activity's viewgroup.
        View view = inflater.inflate(R.layout.fragment_main, container, false);


        //fileNameList will store the flag-image file names for currently enabled
        //regions and countries in the current quiz.
        fileNameList = new ArrayList<>();
        quizCountriesList = new ArrayList<>();
        random = new SecureRandom();
        //handler we will use to delay by 2 seconds the appearance of the next flag after
        //the user corrently guesses the current flag.
        handler = new Handler();

        //load the shake animation thats used for incorrect answers
        shakeAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.incorrect_shake);
        shakeAnimation.setRepeatCount(3); //animation repeats 3 times

        //get references to GUI components
        answerTextView = (TextView) view.findViewById(R.id.answerTextView);
        quizLinearLayout = (LinearLayout) view.findViewById(R.id.quizLinearLayout);
        questionNumberTextView = (TextView) view.findViewById(R.id.questionNumberTextView);
        flagImageView = (ImageView) view.findViewById(R.id.flagImageView);


        //Here we get references to various GUI components that we'll manipulate.
        guessLinearLayouts = new LinearLayout[4];
        guessLinearLayouts[0] = (LinearLayout) view.findViewById(R.id.row1LinearLayout);
        guessLinearLayouts[1] = (LinearLayout) view.findViewById(R.id.row2LinearLayout);
        guessLinearLayouts[2] = (LinearLayout) view.findViewById(R.id.row3LinearLayout);
        guessLinearLayouts[3] = (LinearLayout) view.findViewById(R.id.row4LinearLayout);


        //configure listeners for the guess Buttons
        //Here we get each guess Button from the 4 guessLinearLayouts and register guessButtonListener
        //as the onClickListener, which handles the event raised when user touches any of the guessButtons.
        for (LinearLayout row : guessLinearLayouts) {
            for (int column = 0; column < row.getChildCount(); column++) {
                Button button = (Button) row.getChildAt(column);
                button.setOnClickListener(guessButtonListener);
            }
        }

        //set questionNumberTextView's text
        questionNumberTextView.setText(getString(R.string.question, 1, FLAGS_IN_QUIZ));

        //return the fragments view for display
        return view;

    }


    //updateGuessRows will update guessRows based on value in sharedPreferences
    //called from the app's MainActivity when the app is launched and each
    //time the user changes the # of guess buttons to display with each flag.
    public void updateGuessRows(SharedPreferences sharedPreferences) {

        //get the number of guess buttons that should be displayed.
        String choices = sharedPreferences.getString(MainActivity.CHOICES, null);

        //here we convert the preferences value to an Integer and divide by 2
        //to determine the value for guessRows, which indicates how many of the
        //guessLinearLayouts should be displayed.
        //Because ex: if there are 8 buttons, then 8/2 = 4 so 4 linearlayouts
        guessRows = Integer.parseInt(choices) / 2;

        //hide all guess button LinearLayouts
        for (LinearLayout layout : guessLinearLayouts)
            layout.setVisibility(View.GONE);

        //display appropriate guess button LinearLayouts
        for (int row = 0; row < guessRows; row++)
            guessLinearLayouts[row].setVisibility(View.VISIBLE);

    }


    //Method updateRegions is called from the app's MainActivity when app is launched
    //and each time user changes the world regions that should be included in the quiz.
    //We update the world regions for quiz based on values in SharedPreferences
    public void updateRegions(SharedPreferences sharedPreferences) {

        //We used the sharedpreferences argument to get the names of all of the enabled regions
        //as a Set, and MainActivity.REGIONS is a constant containing the name of the preference,
        //in which the SettingsActivityFragments stores the enabled world regions.
        regionsSet = sharedPreferences.getStringSet(MainActivity.REGIONS, null);
    }


    //resetQuiz() sets up and starts a quiz
    public void resetQuiz() {

        //use AssetManager to get image file names for enabled regions
        //we used AssetManager to acess the folders contents
        AssetManager assets = getActivity().getAssets();
        //then we clear fileNameList to prepate to loadimage file names for
        //only the enabled regions
        fileNameList.clear(); //empty list of image file names


        //now we iterate through all the enabled world regions, for each we use the
        //assetmanagers list method to get an array of the flag-image file names
        //which we store in the String array "paths".
        try {
            //loop through each region
            for (String region : regionsSet) {
                //get a list of all flag image files in this region
                String[] paths = assets.list(region);

                //here we remove the .png extension from each file name
                //and place the names into fileNameList
                for (String path : paths)
                    fileNameList.add(path.replace(".png", ""));

            }
        } //AssetManagers list method throws an exception that we must catch
        catch (IOException exception) {
            Log.e(TAG, "Error loading image file names", exception);
        }

        //reset the number of correct answers made
        correctAnswers = 0;
        //reset the total number of guesses the user made
        totalGuesses = 0;
        //clear prior list of quiz countries
        quizCountriesList.clear();

        int flagCounter = 1;
        int numberOfFlags = fileNameList.size();

        //add FLAGS_IN_QUIZ random file names to the quizCountriesList
        //we add 10 FLAGS_IN_QUIZ randomly selected file names to quizCountriesList
        while (flagCounter <= FLAGS_IN_QUIZ) {
            int randomIndex = random.nextInt(numberOfFlags);

            //get the random file name
            String filename = fileNameList.get(randomIndex);

            //if the region is enabled it hasnt already been chosen
            if (!quizCountriesList.contains(filename)) {
                quizCountriesList.add(filename); //add the file to the list
                ++flagCounter;
            }
        }

        //start the quiz by loading the first flag
        loadNextFlag();
    }


    //Method loadNextFlag loads and displays the next flag and the corresponding
    //set of answer Buttons. The image file names in quizCountriesList have the
    //format: regionName-countryName without the .png extension
    private void loadNextFlag() {
        //get file name of the next flag and remove it from the list
        //We remove the firstname from quizCountriesList and store it
        //in nextImage, we call save it in correctAnswer to later determine
        //whether the user made the right guess.
        String nextImage = quizCountriesList.remove(0);
        //update the correct answer
        correctAnswer = nextImage;

        //clear the answerTextView
        //Next we clear the textview and display the current question number
        answerTextView.setText("");
        //display the current question number
        questionNumberTextView.setText(getString(R.string.question, (correctAnswers + 1), FLAGS_IN_QUIZ));


        //extract the region from the next image's name
        //we extract from nextImage the region to be used as the assets subfolder name
        //from which we'll load the image.
        String region = nextImage.substring(0, nextImage.indexOf('-'));

        //Next we get assetmanager, then use it in the "try-with-resources"
        //statement to open an inputStream to read bytes from the flag images file.
        //useAssetManager to load next image from assets folder.
        //We use that stream as an argument to class Drawables static method createFromStream
        //which creates a Drawable object. Then we set it as flagImageViews item to display
        //by calling its setImageDrawable method.
        AssetManager assets = getActivity().getAssets();

        //get an InputStream to the asset representing the next flag
        //and try to use the InputStream
        try (InputStream stream = assets.open(region + "/" + nextImage + ".png")) {
            //load the asset as a Drawable and display on the flagImageView
            Drawable flag = Drawable.createFromStream(stream, nextImage);
            flagImageView.setImageDrawable(flag);
            //animage the flag onto the screen
            //Next we call the animate method with false to animate the next flag
            //and answer buttons onto the screen.
            animate(false);
        } catch (IOException exception) {
            Log.e(TAG, "Error loading" + nextImage, exception);
        }


        //Then we shuffle the fileNameList
        Collections.shuffle(fileNameList);

        //put the correct answer at the end of the fileNameList
        int correct = fileNameList.indexOf(correctAnswer);
        fileNameList.add(fileNameList.remove(correct));

        //add 2,4,6,or 8 guess Buttons based on the value of guessRows
        //Here we iterate through the Buttons in guessLinearLayouts for the current number
        //of guessRows.
        //For each button: get a reference to the next button, enable the button,
        //get the flag file name from the fileNameList, set the Buttons text with the countryname
        for (int row = 0; row < guessRows; row++){
            //place buttons in currentTablerow
            for (int column = 0; column < guessLinearLayouts[row].getChildCount(); column++){
                //get reference to button to configure
                Button newGuessButton = (Button) guessLinearLayouts[row].getChildAt(column);
                newGuessButton.setEnabled(true);


                //get country name and set it as newGuessbuttons'text
                String filename = fileNameList.get((row * 2) + column);
                newGuessButton.setText(getCountryName(filename));
            }
        }


        //randomly replace one Button with the correct Answer
        int row = random.nextInt(guessRows); //pick random row
        int column = random.nextInt(2); //pick random col
        LinearLayout randomRow = guessLinearLayouts[row]; //get the row
        String countryName = getCountryName(correctAnswer);
        ((Button) randomRow.getChildAt(column)).setText(countryName);
    }



    //Method getCountryName parses the country name from the image file name.
    private String getCountryName(String name){
        //First we get a substring starting from the dash(-) that seperates the region from the
        //country name. Then we call String method replace to replace the underscores with spaces.
        //If a regionName or countryName contains multiple words, theyre seperated by underscores.
        return name.substring(name.indexOf('-') + 1).replace('_', ' ');
    }

    //Method Animate executes the circular reveal animation on the entire layout of the quiz
    //to tranition between questions.
    private void animate(boolean animateOut){

        //prevent animation into the UI for the first flag
        //Here we return immediately for the first question to allow the first question
        //to just appear rather than animate onto the screen.
        if (correctAnswers == 0)
            return;

        //Then we calculate the screen coordinates of the center of the quiz UI.
        //calculate center x and center y
        int centerX = (quizLinearLayout.getLeft() + quizLinearLayout.getRight()) / 2;
        int centerY = (quizLinearLayout.getTop() + quizLinearLayout.getBottom()) / 2;

        //Then we calculate the max radius of the circle in the animation.
        //calculate animation radius
        int radius = Math.max(quizLinearLayout.getWidth(), quizLinearLayout.getHeight());

        Animator animator;

        //The animate method accepts one parameter, "animateOut", we use it to determine
        //whther the animation will show or hide the quiz.

        //If true, we will animate the quizLinearLayout off the screen.
        if (animateOut){
            //create circular reveal animation
            //We create a circular reveal object which takes 5 parameters.
            //first: specify the view on which to apply the animation.
            //second/third: provide the x/y coordinates of the animation circles center.
            //the last 2 determine the starting/ending radion of the animation circle.
            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout, centerX,centerY,radius,0);


            //We create and associate an AnimatorListenerAdapter
            //to call the onAnimationEnd method so when the animation finishes it loads the next flag
            animator.addListener(new AnimatorListenerAdapter() {
                //called when the animation finishes
                @Override
                public void onAnimationEnd(Animator animation) {
                    loadNextFlag();
                }
            });
        } //if false, the method will animate the quizLinearLayout onto the screen at the start of
        //the next question.
        else {
            //if the quizLinearLayout should animate in
            //this causes the quizLinearLayout to animate onto the screen rather than off the screen
            animator = ViewAnimationUtils.createCircularReveal(quizLinearLayout,centerX,centerY,0,radius);
        }

        //set animation duration to 500ms
        animator.setDuration(500);
        //start the animation
        animator.start();
    }

    //Anonymous inner class guessButtonListen implements OnClickListener
    //guessButtonListener is the event handling object for each guess Button,
    //it refers to an anon-inner-class object that implements interface OnClickListener to respond
    //to button events.
    //It receives the clicked Button as a parameter "v".
    private OnClickListener guessButtonListener = new OnClickListener() {
        @Override
        public void onClick(View v) {

            Button guessButton = ((Button) v);
            //We get the buttons texts
            String guess = guessButton.getText().toString();
            //and the parsed country's name
            String answer = getCountryName(correctAnswer);
            //increment number of guesses the user has made
            ++totalGuesses;

            if (guess.equals(answer)){
                //if the guess is correct increment number of correct answers
                ++correctAnswers;

                //display correct answer in green text
                answerTextView.setText(answer + "!");
                answerTextView.setTextColor(getResources().getColor(R.color.correct_answer));

                //disable all guess buttons
                disableButtons();

                //if the user has correct identified FLAGS_IN_QUIZ flags
                //if correctAnswers is FLAGS_IN_QUIZ the quiz is over
                if (correctAnswers == FLAGS_IN_QUIZ){
                    //DialogFragment to display quiz stats and start new quiz
                    //Here we create a new anon-inner-class object that extends DialogFragment and will be
                    //used to display quiz results.
                    //ignore the warning.
                    DialogFragment quizResults = new DialogFragment(){
                        //create an AlertDialog and return in
                        //Here the onCreateDialog method uses an AlterDialog.Builder to config and create
                        //an AlterDialog for showing quiz results, then return it.
                        @Override
                        public Dialog onCreateDialog(Bundle bundle){
                            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                            builder.setMessage(getString(R.string.results, totalGuesses, (1000/(double) totalGuesses)));
                            //this dialog box is not cancellable so user has to interact with it
                            builder.setCancelable(false);

                            //Reset quiz button
                            //When the user touches this dialogs reset quiz button, method resetquiz is called
                            //to start a new game.
                            //In this AlertDialog we need only button that allows the user to acknowledge the message
                            //displayed and reset the quiz. This is known as the positivebutton.
                            //method setPosButton receives the button's label, specified by r.string.reset_quiz,
                            //and a reference to the button's event handler.
                            //In this case we provide an object of an anon-inner-class DialogInterface.OnClickListener
                            builder.setPositiveButton(R.string.reset_quiz, new DialogInterface.OnClickListener(){
                                //We override onClick to respond to the event when the user touches the corresponding button.
                                public void onClick(DialogInterface dialog, int id){
                                    resetQuiz();
                                }
                            });
                            return builder.create(); //return the AlertDialog
                        }
                    };



                    //use FragmentManager to display the DialogFragment
                    //use DialogFragment's show method to display it
                    quizResults.show(getFragmentManager(), "quiz results");
                }
                else {
                    //if correctAnswers is less than FLAGS_IN_QUIZ
                    //answer is correct but quiz is not over
                    //load the next flag after a 2second delay
                    //The first argument defines an anon-inner-class that implements
                    //the Runnable interface, this represents the task to perform,
                    //the second argument is the delay in milliseconds.
                    handler.postDelayed(
                            new Runnable() {
                                @Override
                                public void run() {
                                    animate(true); //animate the flag off the screen
                                }
                            }, 2000); //2000millisecond for 2second delay
                }
            }
            else {
                //answer was incorrect
                flagImageView.startAnimation(shakeAnimation); //play shake
                //display "Incorrect" in red
                answerTextView.setText(R.string.incorrect_answer);
                answerTextView.setTextColor(getResources().getColor(R.color.incorrect_answer));
                //disable incorrect answer
                guessButton.setEnabled(false);
            }
        }
    };



    //method disableButtons iterates through the guess Buttons and disables them.
    //This method is called when the user makes a correct guess.
    private void disableButtons(){
        for (int row = 0; row < guessRows; row++){
            LinearLayout guessRow = guessLinearLayouts[row];
            for (int i = 0; i < guessRow.getChildCount(); i++)
                guessRow.getChildAt(i).setEnabled(false);
        }
    }





}



