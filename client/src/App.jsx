import { useEffect, useMemo, useState } from 'react'

const API_URL = import.meta.env.VITE_API_URL || (import.meta.env.DEV ? '' : 'http://localhost:8090')
const initialForm = { firstName: '', lastName: '', email: '', password: '' }

async function api(path, options = {}, token) {
  const response = await fetch(`${API_URL}${path}`, {
    ...options,
    headers: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(options.headers || {})
    }
  })
  const text = await response.text()
  const data = text ? JSON.parse(text) : null
  if (!response.ok) throw new Error(data?.message || data?.error || `Request failed (${response.status})`)
  return data
}

function loadSession() {
  try { return JSON.parse(localStorage.getItem('smart-support-session')) || null } catch { return null }
}

function App() {
  const [session, setSession] = useState(loadSession)
  const [authMode, setAuthMode] = useState('login')
  const [authForm, setAuthForm] = useState(initialForm)
  const [authError, setAuthError] = useState('')
  const [activeView, setActiveView] = useState('overview')
  const [users, setUsers] = useState([])
  const [userGroup, setUserGroup] = useState('CUSTOMER')
  const [selectedUser, setSelectedUser] = useState(null)
  const [query, setQuery] = useState('')
  const [notice, setNotice] = useState('')
  const [loading, setLoading] = useState(false)
  const [tickets, setTickets] = useState([])
  const [ticketForm, setTicketForm] = useState({ title: '', description: '' })
  const [ticketReplies, setTicketReplies] = useState({})
  const [supportForm, setSupportForm] = useState({ firstName: '', lastName: '', email: '' })

  const role = session?.user?.role || 'CUSTOMER'
  const isAdmin = role === 'ADMIN'
  const isAgent = role === 'SUPPORT_AGENT'
  const canManageUsers = isAdmin || isAgent
  const displayName = session?.user ? `${session.user.firstName} ${session.user.lastName}` : ''
  const spaceTheme = isAdmin ? 'admin' : isAgent ? 'support' : 'customer'
  const roleLabel = isAdmin ? 'Admin workspace' : isAgent ? 'Support workspace' : 'Customer workspace'

  useEffect(() => {
    if (session) localStorage.setItem('smart-support-session', JSON.stringify(session))
    else localStorage.removeItem('smart-support-session')
  }, [session])

  const navItems = useMemo(() => [
    { id: 'overview', label: 'Overview', note: 'Workspace pulse' },
    { id: 'tickets', label: 'Tickets', note: isAgent || isAdmin ? 'Support queue' : 'Your requests' },
    { id: 'profile', label: 'My profile', note: 'Personal details' },
    ...(canManageUsers ? [{ id: 'directory', label: 'User directory', note: isAdmin ? 'Full administration' : 'Support access' }] : [])
  ], [canManageUsers, isAdmin])

  useEffect(() => {
    if (session) loadTickets()
  }, [session])

  async function submitAuth(event) {
    event.preventDefault()
    setAuthError('')
    setLoading(true)
    try {
      const path = authMode === 'login' ? '/api/auth/login' : '/api/auth/register'
      const body = authMode === 'login' ? { email: authForm.email, password: authForm.password } : authForm
      const data = await api(path, { method: 'POST', body: JSON.stringify(body) })
      setSession(data)
      setAuthForm(initialForm)
    } catch (error) { setAuthError(error.message) } finally { setLoading(false) }
  }

  async function fetchUser(id = session?.user?.id) {
    if (!id) return
    setLoading(true)
    try {
      const response = await api(`/api/users/${id}`, {}, session.accessToken)
      setSelectedUser(response.data)
      setNotice('Profile synchronized with User Service.')
    } catch (error) { setNotice(error.message) } finally { setLoading(false) }
  }

  async function loadDirectory(search = query, group = userGroup) {
    setLoading(true)
    try {
      const path = search
        ? `/api/users/search?query=${encodeURIComponent(search)}`
        : `/api/users/role/${group}`
      const response = await api(path, {}, session.accessToken)
      const loadedUsers = Array.isArray(response.data) ? response.data : []
      setUsers(loadedUsers.filter(user => user && user.role === group))
      setNotice(`${loadedUsers.filter(user => user && user.role === group).length} ${group.toLowerCase().replace('_', ' ')} users loaded.`)
    } catch (error) { setNotice(error.message) } finally { setLoading(false) }
  }

  async function loadTickets() {
    try {
      const path = isAgent || isAdmin ? '/api/tickets' : `/api/tickets/customer/${session.user.id}`
      const response = await api(path, {}, session.accessToken)
      setTickets(Array.isArray(response?.data) ? response.data : [])
    } catch (error) { setNotice(error.message) }
  }

  async function createTicket(event) {
    event.preventDefault()
    setLoading(true)
    try {
      await api('/api/tickets', {
        method: 'POST',
        body: JSON.stringify({ ...ticketForm, customerId: session.user.id, customerName: displayName, customerEmail: session.user.email })
      }, session.accessToken)
      setTicketForm({ title: '', description: '' })
      setNotice('Ticket created. AI analysis is running in the background.')
      await loadTickets()
    } catch (error) { setNotice(error.message) } finally { setLoading(false) }
  }

  async function updateTicketStatus(id, status) {
    try {
      await api(`/api/tickets/${id}/status`, { method: 'PATCH', body: JSON.stringify({ status }) }, session.accessToken)
      await loadTickets()
      setNotice(`Ticket marked ${status.toLowerCase().replace('_', ' ')}.`)
    } catch (error) { setNotice(error.message) }
  }

  async function sendTicketReply(id) {
    const resolution = (ticketReplies[id] || '').trim()
    if (!resolution) {
      setNotice('Write a response before sending it to the customer.')
      return
    }
    try {
      await api(`/api/tickets/${id}/resolution`, { method: 'PATCH', body: JSON.stringify({ resolution }) }, session.accessToken)
      setTicketReplies({ ...ticketReplies, [id]: '' })
      await loadTickets()
      setNotice('Final response sent to the customer.')
    } catch (error) { setNotice(error.message) }
  }

  async function assignTicket(id) {
    try {
      await api(`/api/tickets/${id}/assign/${session.user.id}`, { method: 'PATCH' }, session.accessToken)
      await loadTickets()
      setNotice('Ticket assigned to you.')
    } catch (error) { setNotice(error.message) }
  }

  async function createSupport(event) {
    event.preventDefault()
    setLoading(true)
    try {
      const response = await api('/api/auth/admin/support', { method: 'POST', body: JSON.stringify(supportForm) }, session.accessToken)
      setSupportForm({ firstName: '', lastName: '', email: '' })
      setNotice(response.emailSent
        ? 'Support account created. Login credentials were sent by email.'
        : 'Support account created, but the credential email could not be sent.')
      await loadDirectory('', 'SUPPORT_AGENT')
    } catch (error) { setNotice(error.message) } finally { setLoading(false) }
  }

  async function updateProfile(event) {
    event.preventDefault()
    setLoading(true)
    try {
      const form = new FormData(event.currentTarget)
      const payload = Object.fromEntries(form.entries())
      const response = await api(`/api/users/${session.user.id}`, { method: 'PUT', body: JSON.stringify(payload) }, session.accessToken)
      setSelectedUser(response.data)
      setSession({ ...session, user: { ...session.user, ...response.data } })
      setNotice('Profile updated successfully.')
    } catch (error) { setNotice(error.message) } finally { setLoading(false) }
  }

  async function changeStatus(id, action) {
    try {
      await api(`/api/users/${id}/${action}`, { method: 'PATCH' }, session.accessToken)
      await loadDirectory(query, userGroup)
      setNotice(`User ${action.replace('-', ' ')}d.`)
    } catch (error) { setNotice(error.message) }
  }

  function logout() { setSession(null); setUsers([]); setSelectedUser(null) }

  if (!session) return <AuthScreen mode={authMode} setMode={setAuthMode} form={authForm} setForm={setAuthForm} error={authError} loading={loading} onSubmit={submitAuth} />

  const profile = selectedUser || session.user
  return (
    <div className={`app-shell ${spaceTheme}`}>
      <aside className="sidebar">
        <div className="brand-row">
          <div className="brand"><span className="brand-mark">S</span><span>smart support</span></div>
          <span className="space-pill">{roleLabel}</span>
        </div>
        <div className="workspace-label">OPERATIONS CONSOLE</div>
        <nav>{navItems.map(item => <button className={activeView === item.id ? 'nav-item active' : 'nav-item'} key={item.id} onClick={() => { setActiveView(item.id); if (item.id === 'profile') fetchUser() }}><span>{item.label}</span><small>{item.note}</small></button>)}</nav>
        <div className="sidebar-bottom"><div className="status-dot"><i /> Services online</div><button className="logout" onClick={logout}>Sign out <span>↗</span></button></div>
      </aside>
      <main className="main-content">
        <header className="topbar"><div><span className="eyebrow">SMART SUPPORT / {role}</span><h1>{activeView === 'overview' ? `Good to see you, ${profile.firstName}.` : navItems.find(item => item.id === activeView)?.label}</h1></div><div className="user-chip"><span className="avatar">{profile.firstName?.[0]}{profile.lastName?.[0]}</span><div><strong>{displayName}</strong><small>{profile.email}</small></div></div></header>
        {notice && <div className="notice" role="status">{notice}<button onClick={() => setNotice('')}>×</button></div>}
        {activeView === 'overview' && <Overview role={role} profile={profile} onProfile={() => { setActiveView('profile'); fetchUser() }} onDirectory={() => { setActiveView('directory'); loadDirectory('') }} />}
        {activeView === 'tickets' && <TicketWorkspace role={role} tickets={tickets} form={ticketForm} setForm={setTicketForm} replies={ticketReplies} setReplies={setTicketReplies} loading={loading} onCreate={createTicket} onRefresh={loadTickets} onAssign={assignTicket} onStatus={updateTicketStatus} onReply={sendTicketReply} />}
        {activeView === 'profile' && <Profile profile={profile} loading={loading} onSubmit={updateProfile} />}
        {activeView === 'directory' && <Directory users={users} query={query} setQuery={setQuery} loading={loading} isAdmin={isAdmin} group={userGroup} supportForm={supportForm} setSupportForm={setSupportForm} onCreateSupport={createSupport} onGroupChange={group => { setUserGroup(group); setQuery(''); loadDirectory('', group) }} onSearch={() => loadDirectory()} onRefresh={() => loadDirectory('', userGroup)} onStatus={changeStatus} />}
      </main>
    </div>
  )
}

function AuthScreen({ mode, setMode, form, setForm, error, loading, onSubmit }) {
  return <div className="auth-screen"><div className="auth-art"><div className="brand light"><span className="brand-mark">S</span><span>smart support</span></div><div className="art-copy"><span className="eyebrow">SERVICE OPERATIONS</span><h1>Make every support moment count.</h1><p>A focused workspace for teams who turn complex requests into clear outcomes.</p></div><div className="art-footer">AUTH SERVICE <span>●</span> USER SERVICE <span>●</span> READY</div></div><div className="auth-panel"><div className="auth-inner"><span className="eyebrow">WELCOME BACK</span><h2>{mode === 'login' ? 'Sign in to your console' : 'Create your workspace account'}</h2><p className="muted">{mode === 'login' ? 'Use your support credentials to continue.' : 'Start with a customer account. Admin roles are managed securely.'}</p><form onSubmit={onSubmit}>{mode === 'register' && <div className="form-row"><Field label="First name" value={form.firstName} onChange={v => setForm({ ...form, firstName: v })} /><Field label="Last name" value={form.lastName} onChange={v => setForm({ ...form, lastName: v })} /></div>}<Field label="Work email" type="email" value={form.email} onChange={v => setForm({ ...form, email: v })} /><Field label="Password" type="password" value={form.password} onChange={v => setForm({ ...form, password: v })} />{error && <div className="error-box">{error}</div>}<button className="primary-button" disabled={loading}>{loading ? 'Connecting...' : mode === 'login' ? 'Enter console' : 'Create account'} <span>→</span></button></form><button className="switch-auth" onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>{mode === 'login' ? 'New to Smart Support? Create an account' : 'Already have an account? Sign in'}</button></div></div></div>
}

function Field({ label, type = 'text', value, onChange }) { return <label className="field"><span>{label}</span><input required type={type} value={value} onChange={event => onChange(event.target.value)} /></label> }

function Overview({ role, profile, onProfile, onDirectory }) {
  const cards = role === 'ADMIN' ? [['03', 'Active spaces', 'Admin, customer, support'], ['01', 'Profile connected', 'Identity synced'], ['100%', 'Platform health', 'Auth and user services']] : role === 'SUPPORT_AGENT' ? [['24', 'Open conversations', 'Across your queue'], ['04', 'Priority follow-ups', 'Need attention today'], ['98%', 'Response quality', 'This week']] : [['01', 'Your profile', 'Up to date'], ['02', 'Support requests', 'Being handled'], ['24/7', 'Care coverage', 'Always available']]
  return <section className="page-section"><div className="hero-band"><div><span className="eyebrow">{role === 'ADMIN' ? 'CONTROL CENTER' : role === 'SUPPORT_AGENT' ? 'SUPPORT DESK' : 'CUSTOMER SPACE'}</span><h2>{role === 'ADMIN' ? 'A clear view of your operation.' : role === 'SUPPORT_AGENT' ? 'Your queue, with less noise.' : 'Your support, in one calm place.'}</h2><p>{role === 'ADMIN' ? 'Manage people, permissions and the health of your support environment.' : role === 'SUPPORT_AGENT' ? 'Keep customer context close and move every request forward.' : 'Track your identity and stay connected to the team helping you.'}</p></div><div className="hero-orbit"><span>SS</span><small>LIVE</small></div></div><div className="metric-grid">{cards.map(card => <div className="metric" key={card[1]}><strong>{card[0]}</strong><span>{card[1]}</span><small>{card[2]}</small></div>)}</div><div className="quick-grid"><button onClick={onProfile} className="action-panel"><span className="panel-index">01</span><strong>Review my profile</strong><small>Read or update your details</small><b>↗</b></button>{role !== 'CUSTOMER' && <button onClick={onDirectory} className="action-panel accent"><span className="panel-index">02</span><strong>Open user directory</strong><small>{role === 'ADMIN' ? 'Manage the whole organization' : 'Find customer context quickly'}</small><b>↗</b></button>}</div></section>
}

function TicketWorkspace({ role, tickets, form, setForm, replies, setReplies, loading, onCreate, onRefresh, onAssign, onStatus, onReply }) {
  const customer = role === 'CUSTOMER'
  return <section className="page-section"><div className="section-heading"><div><span className="eyebrow">{customer ? 'CUSTOMER / REQUESTS' : 'OPERATIONS / QUEUE'}</span><h2>{customer ? 'Your support requests' : 'Ticket queue'}</h2></div><button className="ghost-button" onClick={onRefresh}>Refresh tickets ↻</button></div>{customer && <form className="ticket-form" onSubmit={onCreate}><div className="form-row"><label className="field"><span>Subject</span><input required minLength="5" value={form.title} onChange={event => setForm({ ...form, title: event.target.value })} /></label></div><label className="field"><span>Describe the issue</span><textarea required minLength="10" rows="4" value={form.description} onChange={event => setForm({ ...form, description: event.target.value })} /></label><button className="primary-button compact" disabled={loading}>{loading ? 'Sending...' : 'Create ticket'} <span>→</span></button></form>}<div className="ticket-list">{tickets.length ? tickets.map(ticket => <article className="ticket-row" key={ticket.id}><div className="ticket-main"><span className={`ticket-status ${ticket.status?.toLowerCase()}`}>{ticket.status?.replaceAll('_', ' ')}</span><h3>{ticket.title}</h3><p>{ticket.description}</p>{!customer && <div className="ai-insight"><strong>AI decision</strong><span>{ticket.category || 'Analysing subject...'} · {ticket.priority || 'Priority pending'}{ticket.aiConfidence ? ` · ${(ticket.aiConfidence * 100).toFixed(0)}% confidence` : ''}</span>{ticket.aiSuggestion && <p>{ticket.aiSuggestion}</p>}</div>}{customer && ticket.resolution && <div className="customer-response"><strong>Final support response</strong><p>{ticket.resolution}</p></div>}</div><div className="ticket-actions">{!customer && !ticket.assignedAgentId && <button className="table-action" onClick={() => onAssign(ticket.id)}>Assign to me</button>}{!customer && ticket.status !== 'RESOLVED' && <><textarea className="reply-box" placeholder="Write the final response to the customer..." value={replies[ticket.id] || ''} onChange={event => setReplies({ ...replies, [ticket.id]: event.target.value })} /><button className="primary-button compact" onClick={() => onReply(ticket.id)}>Send response</button></>}{customer && ticket.status === 'RESOLVED' && <span className="response-sent">Response received</span>}</div></article>) : <div className="empty-state">No tickets yet.</div>}</div></section>
}

function Profile({ profile, loading, onSubmit }) {
  return <section className="page-section narrow"><div className="section-heading"><div><span className="eyebrow">IDENTITY RECORD</span><h2>Your profile</h2></div><span className="record-status"><i /> Active account</span></div><form className="profile-form" onSubmit={onSubmit}><div className="profile-header"><div className="profile-avatar">{profile.firstName?.[0]}{profile.lastName?.[0]}</div><div><h3>{profile.firstName} {profile.lastName}</h3><p>{profile.email}</p></div></div><div className="profile-fields"><label className="field"><span>First name</span><input name="firstName" defaultValue={profile.firstName} required /></label><label className="field"><span>Last name</span><input name="lastName" defaultValue={profile.lastName} required /></label><label className="field"><span>Email</span><input name="email" type="email" defaultValue={profile.email} required /></label><label className="field"><span>Phone</span><input name="phone" defaultValue={profile.phone || ''} /></label><label className="field"><span>Department</span><input name="department" defaultValue={profile.department || ''} /></label><label className="field"><span>Job title</span><input name="jobTitle" defaultValue={profile.jobTitle || ''} /></label><label className="field full"><span>Bio</span><textarea name="bio" defaultValue={profile.bio || ''} rows="4" /></label><label className="field full"><span>Avatar URL</span><input name="avatarUrl" defaultValue={profile.avatarUrl || ''} /></label></div><button className="primary-button compact" disabled={loading}>{loading ? 'Saving...' : 'Save profile'} <span>→</span></button></form></section>
}

function Directory({ users, query, setQuery, loading, isAdmin, group, supportForm, setSupportForm, onCreateSupport, onGroupChange, onSearch, onRefresh, onStatus }) {
  return <section className="page-section"><div className="section-heading"><div><span className="eyebrow">PEOPLE / {isAdmin ? 'ADMIN' : 'SUPPORT'}</span><h2>User directory</h2></div><button className="ghost-button" onClick={onRefresh}>Refresh list ↻</button></div>{isAdmin && <form className="support-create" onSubmit={onCreateSupport}><div><span className="eyebrow">NEW SUPPORT AGENT</span><h3>Create a support account</h3><p>A secure password will be generated and sent to the support email.</p></div><div className="support-create-fields"><input required placeholder="First name" value={supportForm.firstName} onChange={event => setSupportForm({ ...supportForm, firstName: event.target.value })} /><input required placeholder="Last name" value={supportForm.lastName} onChange={event => setSupportForm({ ...supportForm, lastName: event.target.value })} /><input required type="email" placeholder="support@example.com" value={supportForm.email} onChange={event => setSupportForm({ ...supportForm, email: event.target.value })} /><button className="primary-button compact" disabled={loading}>Create and email credentials <span>→</span></button></div></form>}<div className="directory-tabs"><button className={group === 'CUSTOMER' ? 'directory-tab active' : 'directory-tab'} onClick={() => onGroupChange('CUSTOMER')}>Customers</button><button className={group === 'SUPPORT_AGENT' ? 'directory-tab active' : 'directory-tab'} onClick={() => onGroupChange('SUPPORT_AGENT')}>Support agents</button></div><div className="directory-toolbar"><input placeholder={`Search ${group === 'CUSTOMER' ? 'customers' : 'support agents'}`} value={query} onChange={event => setQuery(event.target.value)} onKeyDown={event => event.key === 'Enter' && onSearch()} /><button className="primary-button compact" onClick={onSearch} disabled={loading}>Search <span>→</span></button></div><div className="table-wrap"><table><thead><tr><th>Person</th><th>Department</th><th>Role</th><th>Status</th>{isAdmin && <th>Actions</th>}</tr></thead><tbody>{users.length ? users.map(user => <tr key={user.id}><td><div className="table-person"><span className="mini-avatar">{user.firstName?.[0]}{user.lastName?.[0]}</span><div><strong>{user.firstName} {user.lastName}</strong><small>{user.email}</small></div></div></td><td>{user.department || 'Unassigned'}</td><td><span className="role-tag">{user.role}</span></td><td><span className={user.active ? 'active-tag' : 'inactive-tag'}>{user.active ? 'Active' : 'Inactive'}</span></td>{isAdmin && <td><button className="table-action" onClick={() => onStatus(user.id, user.active ? 'deactivate' : 'activate')}>{user.active ? 'Deactivate' : 'Activate'}</button></td>}</tr>) : <tr><td colSpan={isAdmin ? 5 : 4} className="empty-state">No users in this group yet.</td></tr>}</tbody></table></div></section>
}

export default App
