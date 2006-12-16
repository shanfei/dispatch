/*
 * Databinder: a simple bridge from Wicket to Hibernate
 * Copyright (C) 2006  Nathan Hamblen nathan@technically.us
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */

/*
 * Note: this class contains code adapted from wicket-contrib-database. 
 */

package net.databinder.conv;

import net.databinder.DataRequestCycle;
import net.databinder.DataStaticService;
import net.databinder.conv.components.IConversationPage;

import org.hibernate.FlushMode;
import org.hibernate.classic.Session;
import org.hibernate.context.ManagedSessionContext;

import wicket.IRequestTarget;
import wicket.Page;
import wicket.Response;
import wicket.protocol.http.WebRequest;
import wicket.protocol.http.WebSession;
import wicket.request.target.component.IBookmarkablePageRequestTarget;
import wicket.request.target.component.listener.AbstractListenerInterfaceRequestTarget;

public class DataConversationRequestCycle extends DataRequestCycle {
	public DataConversationRequestCycle(final WebSession session, final WebRequest request, final Response response) {
		super(session, request, response);
	}
	
	@Override
	protected void onBeginRequest() {
		// do nothing, we have to wait until the target is determined
	}
	
	@SuppressWarnings("unchecked")
	public void openSessionFor(IRequestTarget target) {
		if (target instanceof AbstractListenerInterfaceRequestTarget) {
			Page page = ((AbstractListenerInterfaceRequestTarget)target).getPage();
			// if continuing a conversation page
			if (page instanceof  IConversationPage) {
				// look for existing session
				org.hibernate.classic.Session sess = ((IConversationPage)page).getConversationSession();

				// if usable session exists, bind and return
				if (sess != null && sess.isOpen()) {
						sess.beginTransaction();
						ManagedSessionContext.bind(sess);
						return;
				}
				// else start new one and set in page
				sess = openHibernateSession();
				sess.setFlushMode(FlushMode.MANUAL);
				((IConversationPage)page).setConversationSession(sess);
				return;
			}
		}
		// start new standard session
		openHibernateSession();
		if (target instanceof IBookmarkablePageRequestTarget) {
			Class pageClass = ((IBookmarkablePageRequestTarget)target).getPageClass();
			// set to manual if we are going to a conv. page
			if (IConversationPage.class.isAssignableFrom(pageClass))
				DataStaticService.getHibernateSession().setFlushMode(FlushMode.MANUAL);
		}
	}
	
	@Override
	protected void onEndRequest() {
		org.hibernate.classic.Session sess = DataStaticService.getHibernateSession();
		if (sess.getTransaction().isActive())
			sess.getTransaction().rollback();
		
		Page page = getResponsePage() ;
		
		if (page != null) {
			// check for current conversational session
			if (page instanceof IConversationPage) {
				IConversationPage convPage = (IConversationPage)page;
				// close if not dirty contains no changes
				if (!sess.isDirty()) {
					sess.close();
					sess = null;
				}
				convPage.setConversationSession(sess);
			} else
				sess.close();
		}		
		ManagedSessionContext.unbind(DataStaticService.getHibernateSessionFactory());
	}

	/** 
	 * Closes and reopens Hibernate session for this Web session. Unrelated models may try to load 
	 * themselves after this point. 
	 */
	@Override
	public Page onRuntimeException(Page page, RuntimeException e) {
		Session sess = DataStaticService.getHibernateSession();
		sess.getTransaction().rollback();
		sess.close();
		openHibernateSession();
		return null;
	}

}
