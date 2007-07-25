/**
 * Copyright (c) 2000-2007 Liferay, Inc. All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.liferay.portlet.messageboards.util;

import com.liferay.portal.kernel.language.LanguageUtil;
import com.liferay.portal.kernel.portlet.LiferayWindowState;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.ContentUtil;
import com.liferay.portal.util.PropsUtil;
import com.liferay.portlet.messageboards.model.MBBan;
import com.liferay.portlet.messageboards.model.MBCategory;
import com.liferay.portlet.messageboards.model.MBMessage;
import com.liferay.portlet.messageboards.model.impl.MBCategoryImpl;
import com.liferay.portlet.messageboards.service.MBCategoryLocalServiceUtil;
import com.liferay.portlet.messageboards.service.MBMessageLocalServiceUtil;
import com.liferay.util.GetterUtil;
import com.liferay.util.Http;
import com.liferay.util.LocaleUtil;
import com.liferay.util.StringUtil;
import com.liferay.util.Validator;

import java.io.IOException;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import javax.portlet.PortletPreferences;
import javax.portlet.PortletURL;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.WindowState;

import javax.servlet.jsp.PageContext;

/**
 * <a href="MBUtil.java.html"><b><i>View Source</i></b></a>
 *
 * @author Brian Wing Shun Chan
 *
 */
public class MBUtil {

	public static final String SMTP_PORTLET_PREFIX = "mb.";

	public static String getBreadcrumbs(
			long categoryId, long messageId, PageContext pageContext,
			RenderRequest req, RenderResponse res)
		throws Exception {

		if (messageId > 0) {
			MBMessage message = MBMessageLocalServiceUtil.getMessage(messageId);

			return getBreadcrumbs(null, message, pageContext, req, res);
		}
		else {
			MBCategory category = null;

			try {
				if ((categoryId > 0) &&
					(categoryId != MBCategoryImpl.DEFAULT_PARENT_CATEGORY_ID)) {

					category = MBCategoryLocalServiceUtil.getCategory(
						categoryId);
				}
			}
			catch (Exception e) {
			}

			return getBreadcrumbs(category, null, pageContext, req, res);
		}
	}

	public static String getBreadcrumbs(
			MBCategory category, MBMessage message, PageContext pageContext,
			RenderRequest req, RenderResponse res)
		throws Exception {

		if ((message != null) && (category == null)) {
			category = message.getCategory();
		}

		PortletURL categoriesURL = res.createRenderURL();

		WindowState windowState = req.getWindowState();

		if (windowState.equals(LiferayWindowState.POP_UP)) {
			categoriesURL.setWindowState(LiferayWindowState.POP_UP);

			categoriesURL.setParameter(
				"struts_action", "/message_boards/select_category");
		}
		else {
			categoriesURL.setWindowState(WindowState.MAXIMIZED);

			categoriesURL.setParameter("struts_action", "/message_boards/view");
			categoriesURL.setParameter(
				"categoryId",
				String.valueOf(MBCategoryImpl.DEFAULT_PARENT_CATEGORY_ID));
		}

		String categoriesLink =
			"<a href=\"" + categoriesURL.toString() + "\">" +
				LanguageUtil.get(pageContext, "categories") + "</a>";

		if (category == null) {
			return categoriesLink;
		}

		String breadcrumbs = StringPool.BLANK;

		if (category != null) {
			for (int i = 0;; i++) {
				PortletURL portletURL = res.createRenderURL();

				if (windowState.equals(LiferayWindowState.POP_UP)) {
					portletURL.setWindowState(LiferayWindowState.POP_UP);

					portletURL.setParameter(
						"struts_action", "/message_boards/select_category");
					portletURL.setParameter(
						"categoryId", String.valueOf(category.getCategoryId()));
				}
				else {
					portletURL.setWindowState(WindowState.MAXIMIZED);

					portletURL.setParameter(
						"struts_action", "/message_boards/view");
					portletURL.setParameter(
						"categoryId", String.valueOf(category.getCategoryId()));
				}

				String categoryLink =
					"<a href=\"" + portletURL.toString() + "\">" +
						category.getName() + "</a>";

				if (i == 0) {
					breadcrumbs = categoryLink;
				}
				else {
					breadcrumbs = categoryLink + " &raquo; " + breadcrumbs;
				}

				if (category.isRoot()) {
					break;
				}

				category = MBCategoryLocalServiceUtil.getCategory(
					category.getParentCategoryId());
			}
		}

		breadcrumbs = categoriesLink + " &raquo; " + breadcrumbs;

		if (message != null) {
			PortletURL messageURL = res.createRenderURL();

			messageURL.setWindowState(WindowState.MAXIMIZED);

			messageURL.setParameter(
				"struts_action", "/message_boards/view_message");
			messageURL.setParameter(
				"messageId", String.valueOf(message.getMessageId()));

			String messageLink =
				"<a href=\"" + messageURL.toString() + "\">" +
					message.getSubject() + "</a>";

			breadcrumbs = breadcrumbs + " &raquo; " + messageLink;
		}

		return breadcrumbs;
	}

	public static String getEmailFromAddress(PortletPreferences prefs) {
		String emailFromAddress = PropsUtil.get(
			PropsUtil.MESSAGE_BOARDS_EMAIL_FROM_ADDRESS);

		return prefs.getValue("email-from-address", emailFromAddress);
	}

	public static String getEmailFromName(PortletPreferences prefs) {
		String emailFromName = PropsUtil.get(
			PropsUtil.MESSAGE_BOARDS_EMAIL_FROM_NAME);

		return prefs.getValue("email-from-name", emailFromName);
	}

	public static boolean getEmailMessageAddedEnabled(
		PortletPreferences prefs) {

		String emailMessageAddedEnabled = prefs.getValue(
			"email-message-added-enabled", StringPool.BLANK);

		if (Validator.isNotNull(emailMessageAddedEnabled)) {
			return GetterUtil.getBoolean(emailMessageAddedEnabled);
		}
		else {
			return GetterUtil.getBoolean(PropsUtil.get(
				PropsUtil.MESSAGE_BOARDS_EMAIL_MESSAGE_ADDED_ENABLED));
		}
	}

	public static String getEmailMessageAddedBody(PortletPreferences prefs)
		throws IOException {

		String emailMessageAddedBody = prefs.getValue(
			"email-message-added-body", StringPool.BLANK);

		if (Validator.isNotNull(emailMessageAddedBody)) {
			return emailMessageAddedBody;
		}
		else {
			return ContentUtil.get(PropsUtil.get(
				PropsUtil.MESSAGE_BOARDS_EMAIL_MESSAGE_ADDED_BODY));
		}
	}

	public static String getEmailMessageAddedSignature(PortletPreferences prefs)
		throws IOException {

		String emailMessageAddedSignature = prefs.getValue(
			"email-message-added-signature", StringPool.BLANK);

		if (Validator.isNotNull(emailMessageAddedSignature)) {
			return emailMessageAddedSignature;
		}
		else {
			return ContentUtil.get(PropsUtil.get(
				PropsUtil.MESSAGE_BOARDS_EMAIL_MESSAGE_ADDED_SIGNATURE));
		}
	}

	public static String getEmailMessageAddedSubjectPrefix(
			PortletPreferences prefs)
		throws IOException {

		String emailMessageAddedSubjectPrefix = prefs.getValue(
			"email-message-added-subject-prefix", StringPool.BLANK);

		if (Validator.isNotNull(emailMessageAddedSubjectPrefix)) {
			return emailMessageAddedSubjectPrefix;
		}
		else {
			return ContentUtil.get(PropsUtil.get(
				PropsUtil.MESSAGE_BOARDS_EMAIL_MESSAGE_ADDED_SUBJECT_PREFIX));
		}
	}

	public static boolean getEmailMessageUpdatedEnabled(
		PortletPreferences prefs) {

		String emailMessageUpdatedEnabled = prefs.getValue(
			"email-message-updated-enabled", StringPool.BLANK);

		if (Validator.isNotNull(emailMessageUpdatedEnabled)) {
			return GetterUtil.getBoolean(emailMessageUpdatedEnabled);
		}
		else {
			return GetterUtil.getBoolean(PropsUtil.get(
				PropsUtil.MESSAGE_BOARDS_EMAIL_MESSAGE_UPDATED_ENABLED));
		}
	}

	public static String getEmailMessageUpdatedBody(PortletPreferences prefs)
		throws IOException {

		String emailMessageUpdatedBody = prefs.getValue(
			"email-message-updated-body", StringPool.BLANK);

		if (Validator.isNotNull(emailMessageUpdatedBody)) {
			return emailMessageUpdatedBody;
		}
		else {
			return ContentUtil.get(PropsUtil.get(
				PropsUtil.MESSAGE_BOARDS_EMAIL_MESSAGE_UPDATED_BODY));
		}
	}

	public static String getEmailMessageUpdatedSignature(
			PortletPreferences prefs)
		throws IOException {

		String emailMessageUpdatedSignature = prefs.getValue(
			"email-message-updated-signature", StringPool.BLANK);

		if (Validator.isNotNull(emailMessageUpdatedSignature)) {
			return emailMessageUpdatedSignature;
		}
		else {
			return ContentUtil.get(PropsUtil.get(
				PropsUtil.MESSAGE_BOARDS_EMAIL_MESSAGE_UPDATED_SIGNATURE));
		}
	}

	public static String getEmailMessageUpdatedSubjectPrefix(
			PortletPreferences prefs)
		throws IOException {

		String emailMessageUpdatedSubject = prefs.getValue(
			"email-message-updated-subject-prefix", StringPool.BLANK);

		if (Validator.isNotNull(emailMessageUpdatedSubject)) {
			return emailMessageUpdatedSubject;
		}
		else {
			return ContentUtil.get(PropsUtil.get(
				PropsUtil.MESSAGE_BOARDS_EMAIL_MESSAGE_UPDATED_SUBJECT_PREFIX));
		}
	}
	
	public static boolean getShowFullName(PortletPreferences prefs) {
		String showFullName = PropsUtil.get(
				PropsUtil.MESSAGE_BOARDS_SHOW_FULLNAME); 
			
		return GetterUtil.getBoolean(
				prefs.getValue("show-fullname", showFullName), true);
	}
	
	public static int getRSSContentLength(PortletPreferences prefs) {
		String rssContentLength = PropsUtil.get(
				PropsUtil.MESSAGE_BOARDS_RSS_CONTENT_LENGTH); 
			
		return GetterUtil.getInteger(
				prefs.getValue("rss-content-length", rssContentLength), 80);
	}	
	
	public static String getMailId(long messageId, String mx) {
		return StringPool.LESS_THAN + messageId + StringPool.PERIOD +
			SMTP_PORTLET_PREFIX + StringPool.AT +
				PropsUtil.get(PropsUtil.SMTP_SERVER_SUBDOMAIN) +
					StringPool.PERIOD + mx + StringPool.GREATER_THAN;
	}

	public static String getMailingListAddress(long categoryId, String mx) {
		return SMTP_PORTLET_PREFIX + categoryId + StringPool.AT +
			PropsUtil.get(PropsUtil.SMTP_SERVER_SUBDOMAIN) + StringPool.PERIOD +
				mx;
	}

	public static long getMessageId(String mailId) {
		int x = mailId.indexOf(StringPool.LESS_THAN) + 1;
		int y = mailId.indexOf(StringPool.PERIOD);

		if ((x > 0 ) && (y != -1)) {
			return GetterUtil.getLong(mailId.substring(x, y));
		}
		else {
			return 0;
		}
	}

	public static String getRanksKey(String languageId) {
		String ranksKey = "ranks";

		String defaultLanguageId = LocaleUtil.toLanguageId(Locale.getDefault());

		if (!languageId.equals(defaultLanguageId)) {
			ranksKey += "_" + languageId;
		}

		return ranksKey;
	}

	public static String[] getThreadPriority(
			PortletPreferences prefs, double value, ThemeDisplay themeDisplay)
		throws Exception {

		String[] priorities = prefs.getValues("priorities", new String[0]);

		for (int i = 0; i < priorities.length; i++) {
			String[] priority = StringUtil.split(priorities[i]);

			try {
				String priorityName = priority[0];
				String priorityImage = priority[1];
				double priorityValue = GetterUtil.getDouble(priority[2]);

				if (value == priorityValue) {
					if (!priorityImage.startsWith(Http.HTTP)) {
						priorityImage =
							themeDisplay.getPathThemeImages() + priorityImage;
					}

					return new String[] {priorityName, priorityImage};
				}
			}
			catch (Exception e) {
			}
		}

		return null;
	}

	public static Date getUnbanDate(MBBan ban, int expireInterval) {
		Date banDate = ban.getCreateDate();

		Calendar cal = Calendar.getInstance();

		cal.setTime(banDate);

		cal.add(Calendar.DATE, expireInterval);

		return cal.getTime();
	}

	public static String getUserRank(
			PortletPreferences prefs, String languageId, int posts)
		throws Exception {

		String rank = StringPool.BLANK;

		String[] ranks = prefs.getValues(
			getRanksKey(languageId), new String[0]);

		for (int i = 0; i < ranks.length; i++) {
			String[] kvp = StringUtil.split(ranks[i], StringPool.EQUAL);

			String kvpName = kvp[0];
			int kvpPosts = GetterUtil.getInteger(kvp[1]);

			if (posts >= kvpPosts) {
				rank = kvpName;
			}
			else {
				break;
			}
		}

		return rank;
	}

}