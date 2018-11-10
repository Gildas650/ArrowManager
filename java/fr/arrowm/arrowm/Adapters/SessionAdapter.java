package fr.arrowm.arrowm.Adapters;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import fr.arrowm.arrowm.Activities.RoundAct;
import fr.arrowm.arrowm.Business.Session;
import fr.arrowm.arrowm.Db.ArrowDataBase;
import fr.arrowm.arrowm.R;

/**
 * Adapter created to creat line in list view on Activity LogAct
 * Created by Gildas on 30/12/2016.
 */

public class SessionAdapter extends ArrayAdapter<Session> {

    private final static String SESSION = "com.arrowM.SESSION";
    private DateFormat df = new SimpleDateFormat("dd/MM/yyyy");
    private ArrowDataBase db;
    private boolean sureTodelete = false;
    private List<Session> list;


    public SessionAdapter(Context context, List<Session> sessions) {
        super(context, 0, sessions);
        this.list = sessions;
        db = new ArrowDataBase(context);
    }

    @NonNull
    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.row_log, parent, false);
        }
        //Initialize the View if it's the first line
        SessionViewHolder viewHolder = (SessionViewHolder) convertView.getTag();
        if (viewHolder == null) {
            viewHolder = new SessionViewHolder();
            viewHolder.pseudo = (TextView) convertView.findViewById(R.id.boldtext);
            viewHolder.text = (TextView) convertView.findViewById(R.id.text);
            viewHolder.avatar = (ImageButton) convertView.findViewById(R.id.avatar);
            viewHolder.delete = (ImageButton) convertView.findViewById(R.id.delete);
            convertView.setTag(viewHolder);
        }

        //Define the text depending on Competition or not
        Session session = getItem(position);
        String bold;
        String text;
        bold = df.format(session.getBeginOfSession());
        if (session.isExistRound()) {
            viewHolder.avatar.setImageResource(R.drawable.iconcomp);
            text = session.getRound().getEvent().getName() + " - " + Math.round(session.getRound().getStats()[0]) + " - " + session.getNumberOfArrows() + " fleches";

        } else {
            viewHolder.avatar.setImageResource(R.drawable.icontrain);
            text = session.toStringSessionDuration() + " - " + session.getNumberOfArrows() + " fleches";
        }


        viewHolder.pseudo.setText(bold);
        viewHolder.text.setText(text);

        //Managed double click button to delete
        viewHolder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (sureTodelete) {
                    db.open();
                    db.removeSession(list.get(position).getDbId());
                    db.close();

                    list.remove(position);
                    ImageView iv = (ImageView) v;
                    iv.setImageResource(R.drawable.delete);
                    sureTodelete = false;
                    notifyDataSetChanged();
                } else {
                    sureTodelete = true;
                    ImageView iv = (ImageView) v;
                    iv.setImageResource(R.drawable.surtodelete);

                }
            }
        });

        //Manage button to go to the Round Activity
        viewHolder.avatar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (list.get(position).isExistRound()) {
                    Intent i = new Intent(parent.getContext(), RoundAct.class);
                    i.putExtra(SESSION, list.get(position));
                    parent.getContext().startActivity(i);
                }
            }
        });

        return convertView;
    }

    private class SessionViewHolder {
        public TextView text;
        TextView pseudo;
        ImageButton avatar;
        ImageButton delete;

    }
}