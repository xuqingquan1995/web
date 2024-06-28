package top.xuqingquan.web.nokernel;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public final class Action implements Parcelable {

    public static final int ACTION_PERMISSION = 1;
    public static final int ACTION_FILE = 2;
    public static final int ACTION_CAMERA = 3;
    public static final int ACTION_VIDEO = 4;
    private List<String> mPermissions = new ArrayList<>();
    private int mAction;
    private int mFromIntention;

    public Action() {
    }

    public List<String> getPermissions() {
        return mPermissions;
    }

    public void setPermissions(List<String> permissions) {
        this.mPermissions = permissions;
    }

    public int getAction() {
        return mAction;
    }

    public void setAction(int action) {
        this.mAction = action;
    }

    private Action(Parcel in) {
        mPermissions = in.createStringArrayList();
        mAction = in.readInt();
        mFromIntention = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeStringList(mPermissions);
        dest.writeInt(mAction);
        dest.writeInt(mFromIntention);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Action> CREATOR = new Creator<>() {
        @Override
        public Action createFromParcel(Parcel in) {
            return new Action(in);
        }

        @Override
        public Action[] newArray(int size) {
            return new Action[size];
        }
    };

    public int getFromIntention() {
        return mFromIntention;
    }

    public static Action createPermissionsAction(List<String> permissions) {
        Action mAction = new Action();
        mAction.setAction(Action.ACTION_PERMISSION);
        mAction.setPermissions(permissions);
        return mAction;
    }

    /** @noinspection UnusedReturnValue*/
    public Action setFromIntention(int fromIntention) {
        this.mFromIntention = fromIntention;
        return this;
    }
}
