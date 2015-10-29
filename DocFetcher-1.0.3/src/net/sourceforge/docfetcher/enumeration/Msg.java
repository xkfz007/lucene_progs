/*******************************************************************************
 * Copyright (c) 2007, 2008 Tran Nam Quang.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Tran Nam Quang - initial API and implementation
 *******************************************************************************/

package net.sourceforge.docfetcher.enumeration;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import net.sourceforge.docfetcher.Const;

/**
 * An enumeration of localized message strings.
 * 
 * @author Tran Nam Quang
 */
public enum Msg {
	
	// Generic
	search_with_docfetcher,
	confirm_operation,
	invalid_operation,
	system_error,
	report_bug,
	read_error,
	write_error,
	write_warning,
	hotkey_in_use,
	invalid_start_params,
	yes,
	no,
	ok,
	cancel,
	force_quit,
	close,
	
	// Filter panel
	filesize_group_label,
	filetype_group_label,
	invert_selection,
	filetype_abi,
	filetype_chm,
	filetype_doc,
	filetype_docx,
	filetype_ppt,
	filetype_pptx,
	filetype_xls,
	filetype_xlsx,
	filetype_vsd,
	filetype_html,
	filetype_odt,
	filetype_ods,
	filetype_odg,
	filetype_odp,
	filetype_pdf,
	filetype_rtf,
	filetype_svg,
	filetype_txt,
	filetype_wpd,
	
	// Search scope panel
	search_scope,
	create_index,
	contains_file,
	invalid_dnd_source,
	update_index,
	rebuild_index,
	remove_index,
	remove_sel_indexes,
	remove_orphaned_indexes,
	remove_orphaned_indexes_msg,
	check_toplevel_only,
	uncheck_toplevel_only,
	check_all,
	uncheck_all,
	open_folder,
	list_docs,
	folders_not_found_title,
	folders_not_found,
	create_subfolder,
	enter_folder_name,
	enter_folder_name_new,
	create_subfolder_failed,
	rename_folder,
	rename_requires_full_rebuild,
	enter_new_foldername,
	cant_rename_folder,
	untitled,
	delete_folder,
	delete_folder_q,
	paste_into_folder,
	toggle_delete_on_exit,
	open_target_folder,
	file_already_exists,
	file_already_exists_dot,
	folder_already_exists,
	folder_not_found,
	file_transfer,
	no_files_in_cb,
	moving_files,
	copying,
	deleting,
	
	// Main panel
	prev_page,
	next_page,
	preferences,
	to_systray,
	occurrence_count,
	prev_occurrence,
	next_occurrence,
	open_manual,
	use_embedded_html_viewer,
	browser_stop,
	browser_refresh,
	browser_launch_external,
	loading,
	cant_read_file,
	preview_limit_hint,
	systray_not_available,
	restore_app,
	exit,
	jobs,
	open_file_error,
	
	// Search
	enter_nonempty_string,
	invalid_query,
	invalid_query_syntax,
	leading_wildcard,
	search_scope_empty,
	minsize_not_greater_maxsize,
	filesize_out_of_range,
	no_filetypes_selected,
	
	// Status bar
	press_help_button,
	num_results,
	num_results_detail,
	page_m_n,
	num_sel_results,
	num_documents_added,
	
	// Preferences
	pref_manual_on_startup,
	pref_close_tabs,
	pref_watch_fs,
	pref_use_or_operator,
	pref_hide_in_systray,
	pref_highlight,
	pref_clear_search_history_on_exit,
	pref_highlight_color,
	pref_text_ext,
	pref_html_ext,
	pref_skip_regex,
	pref_max_results,
	pref_max_results_range,
	keybox_title,
	keybox_msg,
	pref_hotkey,
	restore_defaults,
	help,
	
	// Indexing config dialog
	scope_folder_title,
	scope_folder_msg,
	index_management,
	target_folder,
	ipref_text_ext,
	ipref_html_ext,
	select_exts,
	ipref_skip_regex,
	ipref_detect_html_pairs,
	ipref_delete_on_exit,
	run,
	regex_matches_file_yes,
	regex_matches_file_no,
	target_folder_deleted,
	not_a_regex,
	add_to_queue,
	inters_indexes,
	inters_queue,
	choose_regex_testfile_title,
	discard_incomplete_index,
	
	// Indexing feedback
	progress,
	html_pairing,
	waiting_in_queue,
	file_skipped,
	finished,
	finished_with_errors,
	total_elapsed_time,
	errors,
	error_type,
	out_of_jvm_memory,
	file_not_found,
	file_not_readable,
	file_corrupted,
	unsupported_encoding,
	doc_pw_protected,
	no_extraction_permission,
	parser_error,
	wordperfect_expected,
	wordperfect_parser_not_installed,
	send_file_for_debugging,
	
	// Result panel
	open,
	open_parent,
	open_limit,
	copy,
	delete_file,
	confirm_delete_file,
	empty_folders,
	empty_folders_msg,
	property_title,
	property_score,
	property_size,
	property_name,
	property_type,
	property_path,
	property_author,
	property_lastModified,
	parser_testbox,
	choose_file,
	enter_path_msg,
	original_parser_output,
	parser_testbox_info,
	parser_testbox_invalid_input,
	unknown_document_format,
	parsing,
	parser_not_supported,
	parsed_by,
	parse_exception,
	parser_testbox_unknown_error,
	
	;
	
	/**
	 * The message string the enumeration item corresponds to.
	 */
	private String value = "(Error: Resources not loaded properly.)"; //$NON-NLS-1$
	
	/**
	 * Returns the localized message string represented by this enumeration.
	 * Returns an error message if the load process during startup has failed.
	 */
	public String value() {
		return value;
	}
	
	/**
	 * Returns a string created from a <tt>java.text.MessageFormat</tt>
	 * with the given argument(s).
	 */
	public String format(Object... obj) {
		return MessageFormat.format(value, obj);
	}
	
	/*
	 * Loads the localized messages from disk.
	 */
	static {
		try {
			String langEnglish = Locale.ENGLISH.getLanguage();
			String langDefault = Locale.getDefault().getLanguage();
			if (langDefault.equals(langEnglish)) {
				ResourceBundle bundle = ResourceBundle.getBundle(Const.RESOURCE_BUNDLE);
				for (Msg msg : Msg.values())
					msg.value = bundle.getString(msg.name());
			}
			else {
				/*
				 * For non-English locales, use the English message for those
				 * messages that haven't been translated yet, indicated by a
				 * "$TODO$" value.
				 */
				ResourceBundle bundleDefault = ResourceBundle.getBundle(Const.RESOURCE_BUNDLE);
				ResourceBundle bundleEnglish = ResourceBundle.getBundle(Const.RESOURCE_BUNDLE, Locale.ROOT);
				for (Msg msg : Msg.values()) {
					msg.value = bundleDefault.getString(msg.name());
					if (msg.value.equals("$TODO$")) //$NON-NLS-1$
						msg.value = bundleEnglish.getString(msg.name());
				}
			}
		} catch (MissingResourceException e) {
			e.printStackTrace();
		}
	}

}
