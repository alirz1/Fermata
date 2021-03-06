package me.aap.fermata.ui.view;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import me.aap.fermata.R;
import me.aap.fermata.media.lib.MediaLib.BrowsableItem;
import me.aap.fermata.media.lib.MediaLib.Item;
import me.aap.fermata.media.lib.MediaLib.PlayableItem;
import me.aap.utils.collection.CollectionUtils;
import me.aap.utils.ui.view.MovableRecyclerViewAdapter;

import static me.aap.utils.collection.CollectionUtils.filterMap;
import static me.aap.utils.misc.Assert.assertTrue;

/**
 * @author Andrey Pavlenko
 */
public class MediaItemListViewAdapter extends MovableRecyclerViewAdapter<MediaItemViewHolder>
		implements OnClickListener {
	private BrowsableItem parent;
	private String filterText = "";
	private Pattern filter;
	private MediaItemListView listView;
	private List<MediaItemWrapper> list = Collections.emptyList();

	@NonNull
	public MediaItemListView getListView() {
		return listView;
	}

	public void setListView(@NonNull MediaItemListView listView) {
		this.listView = listView;
	}

	public BrowsableItem getParent() {
		return parent;
	}

	@CallSuper
	public void setParent(BrowsableItem parent) {
		this.parent = parent;

		if (parent != null) {
			list = Collections.emptyList();
			List<?>[] result = new List[1];
			Pattern f = filter;

			parent.getChildren(c -> {
				if (parent != this.parent) return;
				result[0] = c;
				setChildren(c);
				notifyDataSetChanged();
			}, c -> {
				assertTrue(result[0] != null);
				if ((c == result[0]) || (parent != this.parent) || (f != filter)) return;
				setChildren(c);
				notifyDataSetChanged();
			});

			if (result[0] == null) notifyDataSetChanged();
		} else {
			list = Collections.emptyList();
			notifyDataSetChanged();
		}
	}

	@CallSuper
	protected void setChildren(List<? extends Item> children) {
		list = filterMap(children, this::filter, (i, c, l) -> l.add(new MediaItemWrapper(c)), ArrayList::new);
		notifyDataSetChanged();
	}

	public void setFilter(String filter) {
		if (!filter.equals(filterText)) {
			filterText = filter;
			this.filter = filter.isEmpty() ? null : Pattern.compile(Pattern.quote(filter), Pattern.CASE_INSENSITIVE);
			setParent(getParent());
		}
	}

	public void reload() {
		getListView().discardSelection();
		setParent(getParent());
	}

	public void refresh() {
		getListView().refresh();
	}

	public List<MediaItemWrapper> getList() {
		return list;
	}

	@CallSuper
	@Override
	protected void onItemDismiss(int position) {
		list.remove(position);
		getParent().updateTitles();
		refresh();
	}

	@CallSuper
	@Override
	protected boolean onItemMove(int fromPosition, int toPosition) {
		MediaItemListView listView = getListView();
		MediaItemViewHolder h = (MediaItemViewHolder) listView.getChildViewHolder(listView.getChildAt(fromPosition));
		h.getItemView().hideMenu();
		CollectionUtils.move(list, fromPosition, toPosition);
		getParent().updateTitles();
		refresh();
		return true;
	}

	@NonNull
	@Override
	public MediaItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		MediaItemView v = (MediaItemView) LayoutInflater.from(parent.getContext()).inflate(R.layout.media_item_view, parent, false);
		v.setClickable(true);
		v.setOnClickListener(this);
		v.setListView(getListView());
		return new MediaItemViewHolder(v);
	}

	@Override
	public void onBindViewHolder(@NonNull MediaItemViewHolder holder, int position) {
		List<MediaItemWrapper> list = getList();
		if (position < list.size()) holder.getItemView().setItemWrapper(list.get(position));
	}

	@Override
	public void onViewRecycled(@NonNull MediaItemViewHolder holder) {
		MediaItemView i = holder.getItemView();
		if (i != null) i.cancelLoading();
	}

	@Override
	public int getItemCount() {
		return getList().size();
	}

	public boolean isLongPressDragEnabled() {
		return filter == null;
	}

	public boolean isItemViewSwipeEnabled() {
		return filter == null;
	}

	@Override
	public void onClick(View v) {
		MediaItemView mi = (MediaItemView) v;
		Item i = mi.getItem();

		if (i instanceof BrowsableItem) {
			setParent((BrowsableItem) i);
		}
	}

	public boolean hasSelectable() {
		for (MediaItemWrapper w : getList()) {
			if (w.isSelectionSupported()) return true;
		}
		return false;
	}

	public boolean hasSelected() {
		for (MediaItemWrapper w : getList()) {
			if (w.isSelected()) return true;
		}
		return false;
	}

	public List<PlayableItem> getSelectedItems() {
		List<MediaItemWrapper> list = getList();
		List<PlayableItem> selection = new ArrayList<>(list.size());
		for (MediaItemWrapper w : list) {
			if (w.isSelected() && (w.getItem() instanceof PlayableItem))
				selection.add((PlayableItem) w.getItem());
		}
		return selection;
	}

	private boolean filter(Item i) {
		return (filter == null) || filter.matcher(i.getTitle()).find();
	}
}
