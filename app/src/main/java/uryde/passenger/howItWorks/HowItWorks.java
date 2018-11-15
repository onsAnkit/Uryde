package uryde.passenger.howItWorks;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import uryde.passenger.R;
import uryde.passenger.Login;
import uryde.passenger.util.CommonMethods;


public class HowItWorks extends AppCompatActivity {

    private TextView text;
    private int[] layouts;
    private Button btnNext;
    private Button btnSkip;
    private ViewPager mPager;
    private LinearLayout dotsLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_how_it_works);

        init();
    }

    /**
     * init method used to initialization
     */
    private void init() {
        text = (TextView) findViewById(R.id.text);
        btnSkip = (Button) findViewById(R.id.btn_skip);
        btnNext = (Button) findViewById(R.id.btn_next);
        mPager = (ViewPager) findViewById(R.id.view_pager);
        dotsLayout = (LinearLayout) findViewById(R.id.layoutDots);
        Button btnStarted = (Button) findViewById(R.id.get_started);

        layouts = new int[]{
                R.layout.welcome_slide2,
                R.layout.welcome_slide3,
                R.layout.welcome_slide4};

        //R.layout.welcome_slide4
        // adding bottom dots
        addBottomDots(0);

        // making notification bar transparent
        changeStatusBarColor();
        MyViewPagerAdapter myViewPagerAdapter = new MyViewPagerAdapter();
        mPager.setAdapter(myViewPagerAdapter);
        mPager.addOnPageChangeListener(viewPagerPageChangeListener);

        btnStarted.setTypeface(CommonMethods.headerFont(HowItWorks.this));
        btnStarted.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(HowItWorks.this, Login.class));
                overridePendingTransition(R.anim.mainfadein, R.anim.splashfadeout);
                finish();
            }
        });
    }

    public void setBtnSkip() {
        overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
        finish();
    }

    public void setBtnNext() {
        int current = getItem(+1);
        if (current < layouts.length) {
            // move to next screen
            mPager.setCurrentItem(current);
        } else {
            overridePendingTransition(R.anim.activity_open_translate, R.anim.activity_close_scale);
            finish();
        }
    }

    private void addBottomDots(int currentPage) {
        TextView[] dots = new TextView[layouts.length];

        int[] colorsActive = getResources().getIntArray(R.array.array_dot_active);
        int[] colorsInactive = getResources().getIntArray(R.array.array_dot_inactive);

        dotsLayout.removeAllViews();
        for (int i = 0; i < dots.length; i++) {
            dots[i] = new TextView(this);
            dots[i].setText(Html.fromHtml("&#8226;"));
            dots[i].setTextSize(35);
            dots[i].setTextColor(colorsInactive[currentPage]);
            dotsLayout.addView(dots[i]);
        }

        if (dots.length > 0)
            dots[currentPage].setTextColor(colorsActive[currentPage]);
        if (currentPage == 0) {
            text.setText(getString(R.string.uryde_los_angeles));
        }
        if (currentPage == 1) {
            text.setText(getString(R.string.uryde_los_angeles));
        }
        if (currentPage == 2) {
            text.setText(getString(R.string.uryde_los_angeles));
        }
        if (currentPage == 3) {
            text.setText(getString(R.string.uryde_los_angeles));
        }
    }

    private int getItem(int i) {
        return mPager.getCurrentItem() + i;
    }

    //	viewpager change listener
    ViewPager.OnPageChangeListener viewPagerPageChangeListener = new ViewPager.OnPageChangeListener() {

        @Override
        public void onPageSelected(int position) {
            addBottomDots(position);

            // changing the next button text 'NEXT' / 'GOT IT'
            if (position == layouts.length - 1) {
                // last page. make button text to GOT IT
                btnNext.setText(getString(R.string.start));
                btnSkip.setVisibility(View.INVISIBLE);
            } else {
                // still pages are left
                btnNext.setText(getString(R.string.next));
                btnSkip.setVisibility(View.VISIBLE);
            }
        }

        @Override
        public void onPageScrolled(int arg0, float arg1, int arg2) {

        }

        @Override
        public void onPageScrollStateChanged(int arg0) {

        }
    };

    /**
     * Making notification bar transparent
     */
    private void changeStatusBarColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#ffffff"));
        }
    }

    /**
     * View pager adapter
     */
    private class MyViewPagerAdapter extends PagerAdapter {
        private LayoutInflater layoutInflater;

        MyViewPagerAdapter() {
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            layoutInflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            View view = layoutInflater.inflate(layouts[position], container, false);
            container.addView(view);

            return view;
        }

        @Override
        public int getCount() {
            return layouts.length;
        }

        @Override
        public boolean isViewFromObject(View view, Object obj) {
            return view == obj;
        }


        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            View view = (View) object;
            container.removeView(view);
        }
    }
}
