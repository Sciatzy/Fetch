package com.fetch.auth.production;

import android.content.Intent;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.viewpager2.widget.ViewPager2;

import com.fetch.auth.production.adapter.OnboardingPagerAdapter;
import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends AppCompatActivity {
    private OnboardingPagerAdapter onboardingPagerAdapter;
    private LinearLayout layoutIndicators;
    private Button btnNext;
    private Button btnSkip;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        layoutIndicators = findViewById(R.id.layoutIndicators);
        btnNext = findViewById(R.id.btnNext);
        btnSkip = findViewById(R.id.btnSkip);
        ViewPager2 viewPager = findViewById(R.id.viewPagerOnboarding);

        setupOnboardingItems();
        viewPager.setAdapter(onboardingPagerAdapter);
        setupIndicators();
        setCurrentIndicator(0);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                setCurrentIndicator(position);
                if (position == onboardingPagerAdapter.getItemCount() - 1) {
                    btnNext.setText("Get Started");
                } else {
                    btnNext.setText("Next");
                }
            }
        });

        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() + 1 < onboardingPagerAdapter.getItemCount()) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            } else {
                navigateToRegistration();
            }
        });

        btnSkip.setOnClickListener(v -> navigateToRegistration());
    }

    private void setupOnboardingItems() {
        List<OnboardingPagerAdapter.OnboardingItem> items = new ArrayList<>();

        items.add(new OnboardingPagerAdapter.OnboardingItem(
                R.drawable.fetch_logo, // fallback to fetch_logo for now
                "Welcome to Fetch",
                "Your ultimate local delivery and errand companion. We bring convenience to your doorstep."
        ));

        items.add(new OnboardingPagerAdapter.OnboardingItem(
                R.drawable.ic_restaurant_menu,
                "Food Delivery",
                "Craving something? Order food directly from local restaurants and get it delivered fast."
        ));

        items.add(new OnboardingPagerAdapter.OnboardingItem(
                R.drawable.ic_receipt,
                "Pay Your Bills",
                "Skip the long lines. Settle all your bills easily through the Fetch app anytime, anywhere."
        ));

        items.add(new OnboardingPagerAdapter.OnboardingItem(
                R.drawable.ic_local_shipping,
                "Fetch & Give Tasks",
                "Need an errand done? Create a task and have our reliable riders fetch or deliver it for you."
        ));

        onboardingPagerAdapter = new OnboardingPagerAdapter(items);
    }

    private void setupIndicators() {
        ImageView[] indicators = new ImageView[onboardingPagerAdapter.getItemCount()];
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layoutParams.setMargins(8, 0, 8, 0);

        for (int i = 0; i < indicators.length; i++) {
            indicators[i] = new ImageView(getApplicationContext());
            indicators[i].setImageDrawable(ContextCompat.getDrawable(
                    getApplicationContext(), R.drawable.bg_indicator_inactive
            ));
            indicators[i].setLayoutParams(layoutParams);
            layoutIndicators.addView(indicators[i]);
        }
    }

    private void setCurrentIndicator(int index) {
        int childCount = layoutIndicators.getChildCount();
        for (int i = 0; i < childCount; i++) {
            ImageView imageView = (ImageView) layoutIndicators.getChildAt(i);
            if (i == index) {
                imageView.setImageDrawable(ContextCompat.getDrawable(
                        getApplicationContext(), R.drawable.bg_indicator_active
                ));
            } else {
                imageView.setImageDrawable(ContextCompat.getDrawable(
                        getApplicationContext(), R.drawable.bg_indicator_inactive
                ));
            }
        }
    }

    private void navigateToRegistration() {
        // Take them to the Registration format (MainActivity with isLoginMode set to false)
        Intent intent = new Intent(OnboardingActivity.this, MainActivity.class);
        intent.putExtra("IS_LOGIN_MODE", false);
        startActivity(intent);
        finish();
    }
}
