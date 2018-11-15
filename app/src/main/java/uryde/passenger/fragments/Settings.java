package uryde.passenger.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import uryde.passenger.ChangePassword;
import uryde.passenger.LandingActivity;
import uryde.passenger.R;
import uryde.passenger.util.Constants;

public class Settings extends Fragment implements View.OnClickListener {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.settings, container, false);
        init(view);
        return view;
    }

    /**
     * method used to initialization
     *
     * @param view contain view
     */
    private void init(View view) {
        TextView help = view.findViewById(R.id.help);
        TextView support = view.findViewById(R.id.support);
        TextView resetPassword = view.findViewById(R.id.reset_password);
        TextView privacyPolicy = view.findViewById(R.id.privacy_policy);
        TextView termsAndConditions = view.findViewById(R.id.terms_condition);

        help.setOnClickListener(this);
        support.setOnClickListener(this);
        resetPassword.setOnClickListener(this);
        privacyPolicy.setOnClickListener(this);
        termsAndConditions.setOnClickListener(this);

        help.setVisibility(View.GONE);
        support.setVisibility(View.GONE);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.help:
                goToAboutPage("help");
                break;
            case R.id.support:
                goToAboutPage("support");
                break;
            case R.id.reset_password:
                goToChangePassword();
                break;
            case R.id.privacy_policy:
                goToAboutPage("privacy-policy");
                break;
            case R.id.terms_condition:
                goToAboutPage("terms-condition-passenger");
                break;
        }
    }

    /**
     * method used to goto about page
     */
    private void goToAboutPage(String text) {
        startActivity(new Intent(getActivity(), About.class).putExtra("text", text));
        getActivity().overridePendingTransition(R.anim.mainfadein, R.anim.splashfadeout);
    }

    /**
     * method used to goto about page
     */
    private void goToChangePassword() {
        LandingActivity.title.setText(getString(R.string.reset_password));
        Constants.exitValue = 9;
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        ft.replace(R.id.content_frame, new ChangePassword());
        ft.commit();
    }
}
