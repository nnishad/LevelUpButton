package com.nightowldevelopers.levelup2;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.multidex.MultiDex;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.reward.RewardItem;
import com.google.android.gms.ads.reward.RewardedVideoAd;
import com.google.android.gms.ads.reward.RewardedVideoAdListener;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesCallbackStatusCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.GamesClientStatusCodes;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.games.RealTimeMultiplayerClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import java.util.Objects;

import static android.content.DialogInterface.*;
import static android.widget.Toast.LENGTH_LONG;
import static android.widget.Toast.LENGTH_SHORT;
import static android.widget.Toast.makeText;

public class MainActivity extends Activity implements
        View.OnClickListener, RewardedVideoAdListener {
    protected static final int RC_LEADERBOARD_UI = 9004;
    final static String TAG = "Level Up 2";
    final static int[] CLICKABLES = {
            R.id.button_invite_players,
            R.id.button_see_invitations, R.id.button_sign_in,
            R.id.button_sign_out,
            R.id.button_single_player_2, R.id.button_instagram, R.id.rating, R.id.developershare
    };
    final static int[] SCREENS = {
            R.id.screen_main, R.id.screen_sign_in,

    };

    private static final int RC_SIGN_IN = 9001;
    private static final int RC_ACHIEVEMENT_UI = 9003;
    protected AchievementsClient mAchievementsClient;
    protected LeaderboardsClient mLeaderboardsClient;


    GoogleSignInAccount mSignedInAccount = null;

    int mCurScreen = -1;
    private InterstitialAd interstitial;
    private RewardedVideoAd mRewardedVideoAd;
    // Client used to sign in with Google APIs
    private GoogleSignInClient mGoogleSignInClient = null;
    // Client used to interact with the real time multiplayer system.
    private RealTimeMultiplayerClient mRealTimeMultiplayerClient = null;
    // Client used to interact with the Invitation system.

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //   billingClient = BillingClient.newBuilder(MainActivity).setListener(this).build();
        MobileAds.initialize(this, getString(R.string.admob_app_id));
        AdRequest adIRequest = new AdRequest.Builder().build();
        keepScreenOn();
        // Prepare the Interstitial Ad Activity
        interstitial = new InterstitialAd(MainActivity.this);

        // Insert the Ad Unit ID
        interstitial.setAdUnitId(getString(R.string.admob_interstitial_id));

        // Interstitial Ad load Request
        interstitial.loadAd(adIRequest);
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // Do something after 5s = 5000ms
                // Prepare an Interstitial Ad Listener
                interstitial.setAdListener(new AdListener() {
                    public void onAdLoaded() {
                        // Call displayInterstitial() function when the Ad loads
                        displayInterstitial();
                    }
                });
            }
        }, 3000);

        mRewardedVideoAd = MobileAds.getRewardedVideoAdInstance(this);
        mRewardedVideoAd.setRewardedVideoAdListener(this);

        loadRewardedVideoAd();

        /* Rewarded Video Ends*/
        AdView mAdView = findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("F32D7EF486D6A4824424BD7682D8C999")  // An example device ID
                .build();
        mAdView.loadAd(adRequest);

        // Create the client used to sign in.
        mGoogleSignInClient = GoogleSignIn.getClient(this, GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN);

        // set up a click listener for everything we care about
        for (int id : CLICKABLES) {
            findViewById(id).setOnClickListener(this);
        }
        startSignInIntent();
        switchToMainScreen();

    }

    private void loadRewardedVideoAd() {
        mRewardedVideoAd.loadAd(getString(R.string.vdo_ad_unit_id), new AdRequest.Builder().build());
    }

    public void displayInterstitial() {
        // If Interstitial Ads are loaded then show else show nothing.
        if (interstitial.isLoaded()) {
            interstitial.show();
        }
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }

    @Override
    public void onResume() {
        mRewardedVideoAd.resume(this);
        super.onResume();
        loadRewardedVideoAd();
        signInSilently();
    }

    @Override
    public void onPause() {
        mRewardedVideoAd.pause(this);
        loadRewardedVideoAd();
        super.onPause();
    }

    @Override
    public void onDestroy() {
        mRewardedVideoAd.destroy(this);
        super.onDestroy();
    }

    /**
     * Start a sign in activity.  To properly handle the result, call tryHandleSignInResult from
     * your Activity's onActivityResult function
     */
    public void startSignInIntent() {
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);


    }


    @Override
    public void onClick(View v) {
        loadRewardedVideoAd();


        Handler handler;
        AlertDialog.Builder builder;


        switch (v.getId()) {

            //case R.id.button_single_player:
            case R.id.button_single_player_2:
                Games.getAchievementsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                        .unlock(getString(R.string.achievement_level_1));
                Games.getLeaderboardsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                        .submitScore(getString(R.string.leaderboard_leaderboard), 2500);
                builder = new AlertDialog.Builder(this);
                builder.setMessage("You'll get your Achievement after this Rewarded Video").setTitle("Redeem Rewards");

                //Setting message manually and performing action on button click
                builder.setMessage("You'll get your Achievement after this Rewarded Video")
                        .setCancelable(false)
                        .setPositiveButton("Yes", new OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // finish();
                                Button buttnanim;
                                Animation animation;
                                animation = AnimationUtils.loadAnimation(getApplicationContext(),
                                        R.anim.fade_in);
                                buttnanim = findViewById(R.id.button_single_player_2);
                                buttnanim.startAnimation(animation);
                                MediaPlayer mPlayer = MediaPlayer.create(MainActivity.this, R.raw.ta_da_sound_click);
                                mPlayer.start();
                                Toast.makeText(getApplicationContext(), "Relax,& Sit Back!! \nYour Achievements are Unlocking", LENGTH_LONG).show();


                                Handler handler = new Handler();
                                handler.postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        // Do something after 5s = 5000ms
                                        if (mRewardedVideoAd.isLoaded()) {
                                            mRewardedVideoAd.show();
                                        }
                                    }
                                }, 100);
                                // Toast.makeText(getApplicationContext(),"you choose yes action for alertbox",Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("No", new OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                //  Action for 'NO' Button
                                dialog.cancel();
                                Toast.makeText(getApplicationContext(), "Reward Unlocking Cancelled", Toast.LENGTH_SHORT).show();
                            }
                        });
                //Creating dialog box
                AlertDialog alert = builder.create();
                //Setting the title manually
                alert.setTitle("Redeem Rewards");
                alert.show();


                break;
            case R.id.button_sign_in:
                Log.d(TAG, "Sign-in button clicked");
                startSignInIntent();
                break;
            case R.id.button_sign_out:

                Log.d(TAG, "Sign-out button clicked");
                signOut();
                makeText(this, "Logout Successfully", LENGTH_SHORT).show();
                switchToScreen(R.id.screen_sign_in);
                break;
            case R.id.button_invite_players:
                showLeaderboard();
                break;
            case R.id.button_see_invitations:
                showAchievements();
                break;
            case R.id.button_instagram:

                Uri uri = Uri.parse("http://instagram.com/nightowldevelopers");
                Intent likeIng = new Intent(Intent.ACTION_VIEW, uri);

                likeIng.setPackage("com.instagram.android");

                try {
                    handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Do something after 5s = 5000ms

                        }
                    }, 5000);

                    startActivity(likeIng);
                    makeText(MainActivity.this, "Follow Us \n& Unlock your Achievement", LENGTH_SHORT).show();
                    Games.getAchievementsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                            .unlock(getString(R.string.achievement_instagram_achievement));
                    Games.getLeaderboardsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                            .submitScore(getString(R.string.leaderboard_leaderboard), 50000);
                    handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Do something after 5s = 5000ms
                            MediaPlayer mPlayer = MediaPlayer.create(MainActivity.this, R.raw.ta_da_sound_click);
                            mPlayer.start();
                            Toast.makeText(MainActivity.this, "Hurrah! Your Instagram Achievement is Unlocked !!", LENGTH_LONG).show();

                        }
                    }, 13000);
                } catch (ActivityNotFoundException e) {
                    makeText(MainActivity.this, "Follow Us \n& Unlock your Achievement", LENGTH_SHORT).show();
                    Games.getAchievementsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                            .unlock(getString(R.string.achievement_instagram_achievement));
                    Games.getLeaderboardsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                            .submitScore(getString(R.string.leaderboard_leaderboard), 50000);
                    handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            // Do something after 5s = 5000ms
                            MediaPlayer mPlayer = MediaPlayer.create(MainActivity.this, R.raw.ta_da_sound_click);
                            mPlayer.start();
                            Toast.makeText(MainActivity.this, "Hurrah! Your Instagram Achievement is Unlocked !!", LENGTH_LONG).show();

                        }
                    }, 13000);
                    startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("http://instagram.com/nightowldevelopers")));

                }
                break;
            case R.id.developershare:
                handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Do something after 5s = 5000ms

                    }
                }, 5000);
                final String developerurl = "4619988116632070762"; // getPackageName() from Context or Activity object
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://dev?id=" + developerurl)));
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/dev?id=" + developerurl)));
                }
                Games.getAchievementsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                        .unlock(getString(R.string.achievement_more_xp));
                Games.getLeaderboardsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                        .submitScore(getString(R.string.leaderboard_leaderboard), 50000);
                break;
            case R.id.rating:
                handler = new Handler();
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // Do something after 5s = 5000ms
                        //  Textinst.setText("Download another XP booster app from more button\n and unlock additional Xp");
                    }
                }, 5000);
                makeText(MainActivity.this, "Give 5-star Rating \n& Check your Achievement", LENGTH_SHORT).show();

                final String appPackageName = getPackageName(); // getPackageName() from Context or Activity object
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)));
                    Games.getAchievementsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                            .unlock(getString(R.string.achievement_rate_on_playstore));
//                    Games.getLeaderboardsClient(this, GoogleSignIn.getLastSignedInAccount(this))
//                            .submitScore(getString(R.string.leaderboard_leaderboard), 50000);
                } catch (android.content.ActivityNotFoundException anfe) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)));
                    Games.getAchievementsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                            .unlock(getString(R.string.achievement_rate_on_playstore));
                }
        }
    }

    /**
     * Try to sign in without displaying dialogs to the user.
     * <p>
     * If the user has already signed in previously, it will not show dialog.
     */
    public void signInSilently() {
        Log.d(TAG, "signInSilently()");
        loadRewardedVideoAd();
        mGoogleSignInClient.silentSignIn().addOnCompleteListener(this,
                new OnCompleteListener<GoogleSignInAccount>() {
                    @Override
                    public void onComplete(@NonNull Task<GoogleSignInAccount> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInSilently(): success");
                            onConnected(task.getResult());
                        } else {
                            Log.d(TAG, "signInSilently(): failure", task.getException());
                            onDisconnected();
                        }
                    }
                });
    }

    public void signOut() {
        Log.d(TAG, "signOut()");

        mGoogleSignInClient.signOut().addOnCompleteListener(this,
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {

                        if (task.isSuccessful()) {
                            Log.d(TAG, "signOut(): success");
                        } else {
                            handleException(task.getException(), "signOut() failed!");
                        }

                        onDisconnected();
                    }
                });
    }

    // Activity is going to the background. We have to leave the current room.
    @Override
    public void onStop() {
        Log.d(TAG, "**** got onStop");

        // if we're in a room, leave it.

        // stop trying to keep the screen on
        stopKeepingScreenOn();

        switchToMainScreen();

        super.onStop();
    }


    @Override
    public boolean onKeyDown(int keyCode, KeyEvent e) {

        return super.onKeyDown(keyCode, e);
    }

    private void handleException(Exception exception, String details) {
        int status = 0;

        if (exception instanceof ApiException) {
            ApiException apiException = (ApiException) exception;
            status = apiException.getStatusCode();
        }

        String errorString = null;
        switch (status) {
            case GamesCallbackStatusCodes.OK:
                break;
            case GamesClientStatusCodes.MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER:
                errorString = getString(R.string.status_multiplayer_error_not_trusted_tester);
                break;
            case GamesClientStatusCodes.MATCH_ERROR_ALREADY_REMATCHED:
                errorString = getString(R.string.match_error_already_rematched);
                break;
            case GamesClientStatusCodes.NETWORK_ERROR_OPERATION_FAILED:
                errorString = getString(R.string.network_error_operation_failed);
                break;
            case GamesClientStatusCodes.INTERNAL_ERROR:
                errorString = getString(R.string.internal_error);
                break;
            case GamesClientStatusCodes.MATCH_ERROR_INACTIVE_MATCH:
                errorString = getString(R.string.match_error_inactive_match);
                break;
            case GamesClientStatusCodes.MATCH_ERROR_LOCALLY_MODIFIED:
                errorString = getString(R.string.match_error_locally_modified);
                break;
            case GamesClientStatusCodes.ERROR:
                errorString = "Please Retry Login";
                break;
            default:
                errorString = getString(R.string.unexpected_status, GamesClientStatusCodes.getStatusCodeString(status));
                break;
        }

        if (errorString == null) {
            return;
        }

        String message = getString(R.string.status_exception_error, details, status, exception);

        new AlertDialog.Builder(MainActivity.this)
                .setTitle("Error")
                .setMessage(message + "\n" + errorString)
                .setNeutralButton(android.R.string.ok, null)
                .show();
    }

    private OnFailureListener createFailureListener() {
        return new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                handleException(e, "There was a problem getting the player id!");
            }
        };
    }


    public void onDisconnected() {
        Log.d(TAG, "onDisconnected()");

        mRealTimeMultiplayerClient = null;

        loadRewardedVideoAd();
        switchToMainScreen();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        loadRewardedVideoAd();
        if (requestCode == RC_SIGN_IN) {

            Task<GoogleSignInAccount> task =
                    GoogleSignIn.getSignedInAccountFromIntent(intent);

            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                onConnected(account);
            } catch (ApiException apiException) {

                onDisconnected();

                startSignInIntent();
            }
        }
        super.onActivityResult(requestCode, resultCode, intent);
    }

    private void onConnected(GoogleSignInAccount googleSignInAccount) {
        Log.d(TAG, "onConnected(): connected to Google APIs");
        if (mSignedInAccount != googleSignInAccount) {

            mSignedInAccount = googleSignInAccount;
            mAchievementsClient = Games.getAchievementsClient(this, googleSignInAccount);
            Games.getGamesClient(MainActivity.this, googleSignInAccount);
            GamesClient gamesClient = Games.getGamesClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)));
            gamesClient.setViewForPopups(findViewById(android.R.id.content));
            gamesClient.setGravityForPopups(Gravity.TOP | Gravity.CENTER_HORIZONTAL);
            mLeaderboardsClient = Games.getLeaderboardsClient(this, googleSignInAccount);

            // update the clients
            mRealTimeMultiplayerClient = Games.getRealTimeMultiplayerClient(this, googleSignInAccount);
            /* mInvitationsClient = Games.getInvitationsClient(MainActivity.this, googleSignInAccount);*/

            // get the playerId from the PlayersClient
            PlayersClient playersClient = Games.getPlayersClient(this, googleSignInAccount);
            playersClient.getCurrentPlayer()
                    .addOnSuccessListener(new OnSuccessListener<Player>() {
                        @Override
                        public void onSuccess(Player player) {
                            player.getPlayerId();

                            switchToMainScreen();
                        }
                    })
                    .addOnFailureListener(createFailureListener());
        }


    }


    void switchToScreen(int screenId) {
        // make the requested screen visible; hide all others.
        for (int id : SCREENS) {
            findViewById(id).setVisibility(screenId == id ? View.VISIBLE : View.GONE);
        }
        mCurScreen = screenId;

        // should we show the invitation popup?
        // no invitation, so no popup
    }

    void switchToMainScreen() {
        if (mRealTimeMultiplayerClient != null) {
            switchToScreen(R.id.screen_main);
        } else {
            switchToScreen(R.id.screen_sign_in);
        }
    }


    void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Clears the flag that keeps the screen on.
    void stopKeepingScreenOn() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void showLeaderboard() {
        Games.getLeaderboardsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .getLeaderboardIntent(getString(R.string.leaderboard_leaderboard))
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        startActivityForResult(intent, RC_LEADERBOARD_UI);
                    }
                });
    }


    private void showAchievements() {
        Games.getAchievementsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .getAchievementsIntent()
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        startActivityForResult(intent, RC_ACHIEVEMENT_UI);
                    }
                });
    }

    @Override
    public void onBackPressed() {
        MainActivity.super.onBackPressed();

    }

    @Override
    public void onRewarded(RewardItem reward) {
        Games.getAchievementsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .unlock(getString(R.string.achievement_level_3));
        Games.getLeaderboardsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .submitScore(getString(R.string.leaderboard_leaderboard), 25000);
        Games.getAchievementsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .unlock(getString(R.string.achievement_level_4));
        Games.getLeaderboardsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .submitScore(getString(R.string.leaderboard_leaderboard), 35000);
        Games.getAchievementsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .unlock(getString(R.string.achievement_level_5));
        Games.getLeaderboardsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .submitScore(getString(R.string.leaderboard_leaderboard), 45000);
        Games.getAchievementsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .unlock(getString(R.string.achievement_level_6));
        Games.getLeaderboardsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .submitScore(getString(R.string.leaderboard_leaderboard), 65000);
        Games.getAchievementsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .unlock(getString(R.string.achievement_level_7));
        Games.getLeaderboardsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .submitScore(getString(R.string.leaderboard_leaderboard), 75000);
        Games.getAchievementsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .unlock(getString(R.string.achievement_level_8));
        Games.getLeaderboardsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .submitScore(getString(R.string.leaderboard_leaderboard), 85000);
        Games.getAchievementsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .unlock(getString(R.string.achievement_level_9));
        Games.getLeaderboardsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .submitScore(getString(R.string.leaderboard_leaderboard), 95000);
        Games.getAchievementsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .unlock(getString(R.string.achievement_the_collector));
        Games.getLeaderboardsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .submitScore(getString(R.string.leaderboard_leaderboard), 100000);
    }

    @Override
    public void onRewardedVideoAdLeftApplication() {
        makeText(this, "Watch Complete Video to Boost More Xp",
                LENGTH_SHORT).show();
        loadRewardedVideoAd();
    }

    @Override
    public void onRewardedVideoAdClosed() {
        // Load the next rewarded video ad.
        loadRewardedVideoAd();
    }

    @Override
    public void onRewardedVideoAdFailedToLoad(int errorCode) {
        //  makeText(this, "Video Failed To Load", LENGTH_SHORT).show();
        onRewardedVideoAdLoaded();
    }

    @Override
    public void onRewardedVideoAdLoaded() {
        loadRewardedVideoAd();
        //  Toast.makeText(this, "onRewardedVideo AdLoaded", Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onRewardedVideoAdOpened() {
        loadRewardedVideoAd();
        //  Toast.makeText(this, "onRewardedVideo AdOpened", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onRewardedVideoStarted() {
        loadRewardedVideoAd();

    }

    @Override
    public void onRewardedVideoCompleted() {
        Games.getAchievementsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .unlock(getString(R.string.achievement_level_2));
        Games.getLeaderboardsClient(this, Objects.requireNonNull(GoogleSignIn.getLastSignedInAccount(this)))
                .submitScore(getString(R.string.leaderboard_leaderboard), 250000);
        // makeText(this, "Congratulation you won 80k Exp Points", LENGTH_SHORT).show();
        // Button btn = findViewById(R.id.button_single_player_2);
        // btn.setEnabled(false);


    }
}
