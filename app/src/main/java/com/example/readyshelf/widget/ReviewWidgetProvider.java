package com.example.readyshelf.widget;

import android.app.Application;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;

import androidx.annotation.NonNull;

import com.example.readyshelf.ReadyShelfApp;

/**
 * The home-screen widget "Review list" (see ReviewWidget). Android calls it when a widget is
 * placed, after the phone restarts or ReadyShelf is updated, and about every 6 hours
 * (res/xml/widget_review_info.xml).
 *
 * <p>Never rename or move this class: placed widgets are tied to its name. Don't start WorkManager
 * work here either, because that can make Android send this update again and again.
 */
public class ReviewWidgetProvider extends AppWidgetProvider {

    @Override
    public void onUpdate(@NonNull Context context, @NonNull AppWidgetManager manager,
                         @NonNull int[] appWidgetIds) {
        // Every placed widget is refreshed, on the widget's own thread. goAsync() keeps this
        // update open until that is done (Android allows about 10 seconds; it takes milliseconds).
        PendingResult update = goAsync();
        ReadyShelfApp.containerOf((Application) context.getApplicationContext())
                .reviewWidget.updateAll(update::finish);
    }
}
