<script>
  import { onMount } from 'svelte';
  import { 
    Plus, Search, Users, Edit2, Trash2, X, Check, User, 
    ChevronRight, Home, Moon, Sun, Eye, Edit, UserPlus 
  } from 'lucide-svelte';
  import { GraphQLClient } from 'graphql-request';
  import { marked } from 'marked';

  const API_URL = 'http://localhost:8080/graphql';
  let client = new GraphQLClient(API_URL);

  // Demo users
  const demoUsers = [
    { id: '11111111-1111-1111-1111-111111111111', name: 'Alice', color: '#6366f1' },
    { id: '22222222-2222-2222-2222-222222222222', name: 'Bob', color: '#10b981' },
    { id: '33333333-3333-3333-3333-333333333333', name: 'Carol', color: '#f59e0b' },
  ];

  // State
  let currentUserId = $state(demoUsers[0].id);
  let currentUser = $derived(demoUsers.find(u => u.id === currentUserId));
  let jwtToken = $state(null);  // Real JWT flow

  let teams = $state([]);
  let notes = $state([]);
  let selectedTeamId = $state(null);
  let searchTerm = $state('');
  let isLoading = $state(false);
  let toasts = $state([]);
  let isDark = $state(false);

  // Modals
  let showNoteModal = $state(false);
  let showAddMemberModal = $state(false);
  let editingNote = $state(null);
  let selectedTeamForMembers = $state(null);
  let currentTeamMembers = $state([]);
  let addRole = $state('MEMBER');

  // Form state
  let noteForm = $state({ title: '', content: '', teamId: '' });
  let notePreviewMode = $state(false); // For markdown preview
  let newMemberId = $state('');

  let filteredNotes = $derived(() => {
    let result = [...notes];
    if (searchTerm) {
      const term = searchTerm.toLowerCase();
      result = result.filter(n => 
        n.title.toLowerCase().includes(term) || 
        n.content.toLowerCase().includes(term)
      );
    }
    return result;
  });

  let currentTeam = $derived(teams.find(t => t.id === selectedTeamId));

  // Dark mode
  function toggleDarkMode() {
    isDark = !isDark;
    document.documentElement.classList.toggle('dark', isDark);
    localStorage.setItem('darkMode', isDark);
  }

  // Toast
  function addToast(message, type = 'success') {
    const id = Date.now();
    toasts = [...toasts, { id, message, type }];
    setTimeout(() => {
      toasts = toasts.filter(t => t.id !== id);
    }, 3500);
  }

  // Auth - Real JWT flow
  async function loginWithJwt(userId) {
    try {
      const resp = await fetch('http://localhost:8080/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId })
      });
      
      if (!resp.ok) throw new Error('Login failed');
      
      const data = await resp.json();
      jwtToken = data.token;
      
      // Update client headers
      client = new GraphQLClient(API_URL, {
        headers: {
          'Authorization': `Bearer ${jwtToken}`,
          'X-User-Id': userId
        }
      });
      
      currentUserId = userId;
      addToast(`Logged in as ${demoUsers.find(u => u.id === userId)?.name}`);
      
      // Reload data with new auth
      await loadTeams();
      await loadNotes();
      
    } catch (e) {
      addToast('Login failed - using demo mode', 'error');
      // Fallback to header
      client = new GraphQLClient(API_URL, {
        headers: {
          'X-User-Id': userId
        }
      });
      currentUserId = userId;
      await loadTeams();
      await loadNotes();
    }
  }

  async function executeQuery(query, variables = {}) {
    isLoading = true;
    try {
      const data = await client.request(query, variables);
      return data;
    } catch (error) {
      console.error('GraphQL Error:', error);
      const msg = error.response?.errors?.[0]?.message || error.message || 'Request failed';
      addToast(msg, 'error');
      throw error;
    } finally {
      isLoading = false;
    }
  }

  // Data loading
  async function loadTeams() {
    const query = `query { myTeams { id name createdBy } }`;
    const data = await executeQuery(query);
    teams = data.myTeams || [];
  }

  async function loadNotes(teamId = null) {
    const query = `
      query($teamId: ID, $limit: Int) {
        myNotes(teamId: $teamId, limit: $limit) {
          id title content teamId createdAt ownerName canModify
        }
      }
    `;
    const data = await executeQuery(query, { teamId: teamId || null, limit: 50 });
    notes = data.myNotes || [];
  }

  async function searchNotes(term) {
    if (!term.trim()) {
      await loadNotes(selectedTeamId);
      return;
    }
    const query = `
      query($query: String!, $teamId: ID) {
        searchNotes(query: $query, teamId: $teamId, limit: 30) {
          id title content teamId createdAt ownerName canModify
        }
      }
    `;
    const data = await executeQuery(query, { query: term, teamId: selectedTeamId });
    notes = data.searchNotes || [];
  }

  // CRUD
  async function createNote() {
    const query = `
      mutation($input: CreateNoteInput!) {
        createNote(input: $input) {
          id title content teamId createdAt ownerName canModify
        }
      }
    `;
    const input = {
      title: noteForm.title,
      content: noteForm.content,
      ...(noteForm.teamId && { teamId: noteForm.teamId })
    };

    const data = await executeQuery(query, { input });
    if (data.createNote) {
      addToast('Note created');
      closeNoteModal();
      await loadNotes(selectedTeamId);
    }
  }

  async function updateNote() {
    const query = `
      mutation($id: ID!, $input: UpdateNoteInput!) {
        updateNote(id: $id, input: $input) {
          id title content ownerName canModify
        }
      }
    `;
    const data = await executeQuery(query, {
      id: editingNote.id,
      input: { title: noteForm.title, content: noteForm.content }
    });
    if (data.updateNote) {
      addToast('Note updated');
      closeNoteModal();
      await loadNotes(selectedTeamId);
    }
  }

  async function deleteNote(id) {
    if (!confirm('Delete this note?')) return;
    const query = `mutation($id: ID!) { deleteNote(id: $id) }`;
    await executeQuery(query, { id });
    addToast('Note deleted');
    await loadNotes(selectedTeamId);
  }

  async function createTeam() {
    const name = prompt('Team name:');
    if (!name) return;
    const query = `mutation($name: String!) { createTeam(name: $name) { id name } }`;
    const data = await executeQuery(query, { name });
    if (data.createTeam) {
      addToast(`Team "${name}" created`);
      await loadTeams();
    }
  }

  // Member management
  async function addMemberToTeam(teamId, userId, role = 'MEMBER') {
    const query = `
      mutation($teamId: ID!, $userId: ID!, $role: Role) {
        addMemberToTeam(teamId: $teamId, userId: $userId, role: $role) {
          userId role
        }
      }
    `;
    await executeQuery(query, { teamId, userId, role });
    addToast('Member added / role updated');
  }

  async function removeMemberFromTeam(teamId, userId) {
    if (!confirm('Remove member?')) return;
    const query = `mutation($teamId: ID!, $userId: ID!) { removeMemberFromTeam(teamId: $teamId, userId: $userId) }`;
    await executeQuery(query, { teamId, userId });
    addToast('Member removed');
  }

  // UI helpers
  function showCreateNote() {
    editingNote = null;
    noteForm = { title: '', content: '', teamId: selectedTeamId || '' };
    notePreviewMode = false;
    showNoteModal = true;
  }

  function editNote(note) {
    editingNote = note;
    noteForm = { title: note.title, content: note.content, teamId: note.teamId || '' };
    notePreviewMode = false;
    showNoteModal = true;
  }

  function closeNoteModal() {
    showNoteModal = false;
    editingNote = null;
    notePreviewMode = false;
  }

  async function saveNote() {
    if (!noteForm.title.trim()) {
      addToast('Title is required', 'error');
      return;
    }
    if (editingNote) {
      await updateNote();
    } else {
      await createNote();
    }
  }

  function selectTeam(teamId) {
    selectedTeamId = teamId;
    loadNotes(teamId);
  }

  function showAllNotes() {
    selectedTeamId = null;
    loadNotes();
  }

  async function switchUser(userId) {
    await loginWithJwt(userId); // Real JWT flow
  }

  async function handleSearch() {
    if (searchTerm.trim()) {
      await searchNotes(searchTerm);
    } else {
      await loadNotes(selectedTeamId);
    }
  }

  async function openTeamManagement(team) {
    selectedTeamForMembers = team;
    currentTeamMembers = await getTeamMembers(team.id);

    const myRole = currentTeamMembers.find(m => m.userId === currentUserId)?.role;
    const canManage = myRole === 'OWNER' || myRole === 'ADMIN' || team.createdBy === currentUserId;

    if (!canManage) {
      addToast('Only team owners and admins can manage members', 'error');
      showAddMemberModal = false;
      selectedTeamForMembers = null;
      currentTeamMembers = [];
      return;
    }

    showAddMemberModal = true;
  }

  async function handleAddMember(selectedUserId, role = 'MEMBER') {
    if (!selectedUserId || !selectedTeamForMembers) return;
    
    try {
      await addMemberToTeam(selectedTeamForMembers.id, selectedUserId, role);
      showAddMemberModal = false;
      selectedTeamForMembers = null;
      await loadTeams();
    } catch (e) {
      // error already toasted in executeQuery
    }
  }

  async function getTeamMembers(teamId) {
    const query = `query($teamId: ID!) { teamMembers(teamId: $teamId) { userId role } }`;
    const data = await executeQuery(query, { teamId });
    return data.teamMembers || [];
  }

  function getMarkdownPreview(content) {
    if (!content) return '';
    try {
      return marked.parse(content);
    } catch (e) {
      return content;
    }
  }

  // Init
  async function initialize() {
    // Load dark mode
    const savedDark = localStorage.getItem('darkMode') === 'true';
    isDark = savedDark;
    if (isDark) document.documentElement.classList.add('dark');

    // Auto login as first user with JWT
    await loginWithJwt(currentUserId);
  }

  onMount(() => {
    initialize();
  });
</script>

<div class="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 transition-colors">
  <!-- Top Bar -->
  <div class="bg-white dark:bg-slate-900 border-b border-slate-200 dark:border-slate-800 sticky top-0 z-50">
    <div class="max-w-7xl mx-auto px-6 h-16 flex items-center justify-between">
      <div class="flex items-center gap-x-3">
        <div class="w-9 h-9 rounded-2xl bg-gradient-to-br from-indigo-600 to-violet-600 flex items-center justify-center">
          <span class="text-white font-bold text-lg">N</span>
        </div>
        <div class="font-semibold text-xl tracking-tight">Notes</div>
      </div>

      <div class="flex items-center gap-x-4">
        <!-- User Switcher with JWT -->
        <div class="flex items-center gap-x-1 bg-slate-100 dark:bg-slate-800 rounded-3xl p-1">
          {#each demoUsers as user}
            <button
              onclick={() => switchUser(user.id)}
              class="px-3.5 py-1.5 text-sm font-medium rounded-[22px] flex items-center gap-x-2 transition-all {currentUserId === user.id 
                ? 'bg-white dark:bg-slate-700 shadow' 
                : 'hover:bg-slate-200 dark:hover:bg-slate-700'}"
            >
              <div class="w-2 h-2 rounded-full" style="background: {user.color}"></div>
              {user.name}
            </button>
          {/each}
        </div>

        <!-- Dark Mode Toggle -->
        <button 
          onclick={toggleDarkMode}
          class="p-2.5 rounded-2xl hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-600 dark:text-slate-300"
          title="Toggle dark mode"
        >
          {#if isDark}
            <Sun size={18} />
          {:else}
            <Moon size={18} />
          {/if}
        </button>
      </div>
    </div>
  </div>

  <div class="max-w-7xl mx-auto px-6 py-8">
    <div class="flex gap-8">
      
      <!-- Sidebar -->
      <div class="w-64 shrink-0">
        <div class="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200 dark:border-slate-800 p-3 shadow-sm">
          <button 
            onclick={showAllNotes}
            class="w-full flex items-center gap-3 px-4 py-3 rounded-2xl mb-1 text-left transition {selectedTeamId === null ? 'bg-indigo-50 dark:bg-indigo-950 text-indigo-700 dark:text-indigo-400 font-semibold' : 'hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300'}"
          >
            <Home size={18} />
            <span>All My Notes</span>
          </button>

          <div class="px-3 pt-3 pb-1">
            <div class="flex items-center justify-between text-xs font-semibold text-slate-500 tracking-widest px-1 mb-2">
              TEAMS
              <button onclick={createTeam} class="text-indigo-500 hover:text-indigo-600 p-1 -mr-1" title="New team">
                <Plus size={14} />
              </button>
            </div>
          </div>

          {#each teams as team}
            <div class="group flex items-center">
              <button 
                onclick={() => selectTeam(team.id)}
                class="flex-1 flex items-center gap-3 px-4 py-3 rounded-2xl text-left transition {selectedTeamId === team.id ? 'bg-indigo-50 dark:bg-indigo-950 text-indigo-700 dark:text-indigo-400 font-semibold' : 'hover:bg-slate-50 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300'}"
              >
                <Users size={18} />
                <span class="truncate">{team.name}</span>
              </button>
              
              <button 
                onclick={() => openTeamManagement(team)}
                class="opacity-0 group-hover:opacity-100 px-2 text-slate-400 hover:text-indigo-500"
                title="Manage team members"
              >
                <UserPlus size={15} />
              </button>
            </div>
          {/each}
        </div>
      </div>

      <!-- Main -->
      <div class="flex-1 min-w-0">
        <div class="flex items-end justify-between mb-6">
          <div>
            <h1 class="text-4xl font-semibold tracking-tighter">
              {selectedTeamId && currentTeam ? currentTeam.name : 'All Notes'}
            </h1>
            <p class="text-slate-500 dark:text-slate-400 mt-1 text-sm">
              {selectedTeamId ? 'Team workspace' : 'Personal + shared notes'}
            </p>
          </div>

          <div class="flex items-center gap-3">
            <div class="relative w-80">
              <div class="absolute left-4 top-3 text-slate-400"><Search size={16} /></div>
              <input 
                bind:value={searchTerm}
                oninput={handleSearch}
                type="text" 
                placeholder="Search notes..."
                class="w-full bg-white dark:bg-slate-900 pl-11 pr-4 py-2.5 rounded-3xl border border-slate-300 dark:border-slate-700 text-sm focus:border-indigo-400 focus:outline-none"
              />
            </div>

            <button onclick={showCreateNote} class="flex items-center gap-x-2 bg-indigo-600 hover:bg-indigo-700 text-white px-5 py-2.5 rounded-3xl text-sm font-semibold transition flex-shrink-0">
              <Plus size={16} />
              <span>New Note</span>
            </button>
          </div>
        </div>

        <!-- Notes -->
        {#if isLoading}
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {#each Array(6)}
              <div class="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-5 animate-pulse">
                <div class="h-4 bg-slate-200 dark:bg-slate-700 rounded w-3/4 mb-4"></div>
                <div class="h-3 bg-slate-100 dark:bg-slate-800 rounded w-full mb-2"></div>
                <div class="h-3 bg-slate-100 dark:bg-slate-800 rounded w-5/6"></div>
              </div>
            {/each}
          </div>
        {:else if filteredNotes().length > 0}
          <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
            {#each filteredNotes() as note (note.id)}
              <div class="note-card group bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-3xl p-5 flex flex-col">
                <div class="flex-1">
                  <div class="flex justify-between items-start gap-3 mb-3">
                    <h3 class="font-semibold text-lg text-slate-900 dark:text-white leading-tight pr-2">
                      {note.title}
                    </h3>
                    {#if note.canModify}
                      <div class="flex gap-1 opacity-0 group-hover:opacity-100 transition">
                        <button onclick={() => editNote(note)} class="p-1.5 text-slate-400 hover:text-indigo-500 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800">
                          <Edit2 size={15} />
                        </button>
                        <button onclick={() => deleteNote(note.id)} class="p-1.5 text-slate-400 hover:text-red-500 rounded-lg hover:bg-slate-100 dark:hover:bg-slate-800">
                          <Trash2 size={15} />
                        </button>
                      </div>
                    {/if}
                  </div>
                  <p class="text-sm text-slate-600 dark:text-slate-400 line-clamp-3 leading-relaxed mb-4">
                    {note.content}
                  </p>
                </div>
                <div class="flex items-center justify-between text-xs pt-3 border-t border-slate-100 dark:border-slate-800">
                  <div class="flex items-center gap-1.5 text-slate-400">
                    <span>{new Date(note.createdAt).toLocaleDateString()}</span>
                    {#if note.ownerName}
                      <span class="text-slate-300 dark:text-slate-500">·</span>
                      <span>by {note.ownerName}</span>
                    {/if}
                  </div>
                  {#if note.teamId}
                    <span class="px-2.5 py-px bg-amber-100 dark:bg-amber-900 text-amber-700 dark:text-amber-300 rounded text-[10px] font-medium">TEAM</span>
                  {:else}
                    <span class="px-2.5 py-px bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 rounded text-[10px] font-medium">PERSONAL</span>
                  {/if}
                </div>
              </div>
            {/each}
          </div>
        {:else}
          <div class="text-center py-16 bg-white dark:bg-slate-900 border border-slate-100 dark:border-slate-800 rounded-3xl">
            <div class="mx-auto w-12 h-12 bg-slate-100 dark:bg-slate-800 rounded-2xl flex items-center justify-center mb-4">
              <Search class="text-slate-400" size={24} />
            </div>
            <p class="text-slate-500">No notes found</p>
          </div>
        {/if}
      </div>
    </div>
  </div>

  <!-- Note Modal with Markdown Preview -->
  {#if showNoteModal}
    <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4" onclick={closeNoteModal}>
      <div class="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-2xl p-7 modal" onclick={(e) => e.stopPropagation()}>
        <div class="flex justify-between items-center mb-5">
          <h2 class="text-xl font-semibold">{editingNote ? 'Edit Note' : 'Create Note'}</h2>
          <button onclick={closeNoteModal} class="text-slate-400 hover:text-slate-600"><X size={20} /></button>
        </div>

        <div class="space-y-5">
          <div>
            <label class="text-sm font-medium text-slate-600 dark:text-slate-300 block mb-1.5">Title</label>
            <input bind:value={noteForm.title} type="text" class="w-full border border-slate-200 dark:border-slate-700 focus:border-indigo-400 outline-none rounded-2xl px-4 py-3 bg-white dark:bg-slate-950" placeholder="Note title">
          </div>

          <!-- Tabs for Edit / Preview -->
          <div>
            <div class="flex border-b border-slate-200 dark:border-slate-700 mb-2">
              <button onclick={() => notePreviewMode = false} class="px-4 py-2 text-sm font-medium border-b-2 { !notePreviewMode ? 'border-indigo-500 text-indigo-600' : 'border-transparent text-slate-500' } flex items-center gap-1.5">
                <Edit size={15} /> Edit
              </button>
              <button onclick={() => notePreviewMode = true} class="px-4 py-2 text-sm font-medium border-b-2 { notePreviewMode ? 'border-indigo-500 text-indigo-600' : 'border-transparent text-slate-500' } flex items-center gap-1.5">
                <Eye size={15} /> Preview
              </button>
            </div>

            {#if !notePreviewMode}
              <textarea bind:value={noteForm.content} rows="8" class="w-full border border-slate-200 dark:border-slate-700 focus:border-indigo-400 outline-none rounded-2xl px-4 py-3 bg-white dark:bg-slate-950 font-mono text-sm" placeholder="Write in Markdown..."></textarea>
            {:else}
              <div class="prose prose-sm dark:prose-invert max-w-none border border-slate-200 dark:border-slate-700 rounded-2xl p-4 bg-slate-50 dark:bg-slate-950 min-h-[180px]">
                {@html getMarkdownPreview(noteForm.content)}
              </div>
            {/if}
          </div>

          <div>
            <label class="text-sm font-medium text-slate-600 dark:text-slate-300 block mb-1.5">Team</label>
            <select bind:value={noteForm.teamId} class="w-full border border-slate-200 dark:border-slate-700 focus:border-indigo-400 outline-none rounded-2xl px-4 py-3 bg-white dark:bg-slate-950">
              <option value="">Personal note</option>
              {#each teams as team}
                <option value={team.id}>{team.name}</option>
              {/each}
            </select>
          </div>
        </div>

        <div class="flex justify-end gap-3 mt-8">
          <button onclick={closeNoteModal} class="px-6 py-2.5 text-sm font-medium text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-2xl">Cancel</button>
          <button onclick={saveNote} class="px-6 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-2xl text-sm font-semibold flex items-center gap-x-2">
            <Check size={16} /> {editingNote ? 'Update' : 'Create'}
          </button>
        </div>
      </div>
    </div>
  {/if}

  <!-- Better Member Picker Modal -->
  {#if showAddMemberModal && selectedTeamForMembers}
    <div class="fixed inset-0 bg-black/50 flex items-center justify-center z-50 p-4">
      <div class="bg-white dark:bg-slate-900 rounded-3xl w-full max-w-md p-6">
        <div class="flex justify-between mb-4">
          <div>
            <h3 class="font-semibold">Manage members: {selectedTeamForMembers.name}</h3>
            <p class="text-sm text-slate-500 dark:text-slate-400">Add users or promote to ADMIN (owner only)</p>
          </div>
          <button onclick={() => { showAddMemberModal = false; selectedTeamForMembers = null; }}><X /></button>
        </div>

        <!-- Role selector for new adds -->
        <div class="mb-4">
          <label class="block text-xs font-medium text-slate-500 mb-1">Role when adding new member:</label>
          <select bind:value={addRole} class="w-full border rounded-xl px-3 py-2 bg-white dark:bg-slate-800">
            <option value="MEMBER">MEMBER - view and edit notes</option>
            <option value="ADMIN">ADMIN - can also add/remove members</option>
          </select>
        </div>

        <!-- Add new members -->
        <div class="mb-2 text-xs font-semibold text-slate-500">Add to team</div>
        <div class="grid grid-cols-1 gap-2 mb-4">
          {#each demoUsers.filter(u => u.id !== currentUserId && !currentTeamMembers.some(m => m.userId === u.id)) as user}
            <button 
              onclick={() => handleAddMember(user.id, addRole)}
              class="flex items-center gap-3 px-4 py-3 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-2xl text-left border border-slate-100 dark:border-slate-800"
            >
              <div class="w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-bold" style="background: {user.color}">
                {user.name[0]}
              </div>
              <div class="flex-1">
                <div class="font-medium">{user.name}</div>
                <div class="text-xs text-slate-400">{user.id}</div>
              </div>
              <div class="text-xs px-2 py-0.5 bg-indigo-100 dark:bg-indigo-900 text-indigo-700 dark:text-indigo-300 rounded">Add as {addRole}</div>
            </button>
          {/each}
        </div>
        {#if demoUsers.filter(u => u.id !== currentUserId && !currentTeamMembers.some(m => m.userId === u.id)).length === 0}
          <p class="text-sm text-slate-500 text-center py-1 mb-4">All demo users are already in this team.</p>
        {/if}

        <!-- Current members / promote to admin -->
        {#if currentTeamMembers.filter(m => m.userId !== currentUserId && m.role === 'MEMBER').length > 0}
          <div class="mb-2 text-xs font-semibold text-slate-500">Promote to ADMIN</div>
          <div class="grid grid-cols-1 gap-2">
            {#each currentTeamMembers.filter(m => m.userId !== currentUserId && m.role === 'MEMBER') as member}
              {@const user = demoUsers.find(u => u.id === member.userId)}
              {#if user}
                <button 
                  onclick={() => handleAddMember(member.userId, 'ADMIN')}
                  class="flex items-center gap-3 px-4 py-3 hover:bg-amber-50 dark:hover:bg-amber-950 rounded-2xl text-left border border-amber-100 dark:border-amber-800"
                >
                  <div class="w-8 h-8 rounded-full flex items-center justify-center text-white text-xs font-bold" style="background: {user.color}">
                    {user.name[0]}
                  </div>
                  <div class="flex-1">
                    <div class="font-medium">{user.name} <span class="text-xs text-amber-600">(MEMBER)</span></div>
                    <div class="text-xs text-slate-400">{user.id}</div>
                  </div>
                  <div class="text-xs px-2 py-0.5 bg-amber-200 dark:bg-amber-800 text-amber-700 dark:text-amber-300 rounded">Promote to ADMIN</div>
                </button>
              {/if}
            {/each}
          </div>
        {/if}

        <p class="text-[10px] text-slate-400 mt-4 text-center">Only team owners can manage members and promote to ADMIN.</p>
      </div>
    </div>
  {/if}

  <!-- Toasts -->
  <div class="fixed bottom-6 right-6 space-y-2 z-[100]">
    {#each toasts as toast (toast.id)}
      <div class="toast px-4 py-2.5 rounded-2xl shadow flex items-center gap-3 text-sm bg-white dark:bg-slate-800 border {toast.type === 'error' ? 'border-red-200 text-red-600' : 'border-slate-200 dark:border-slate-700'}">
        {toast.message}
      </div>
    {/each}
  </div>
</div>