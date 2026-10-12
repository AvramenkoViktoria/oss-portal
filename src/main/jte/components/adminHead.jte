@param String active
<div class="breadcrumbs">Адміністрування</div>
<nav class="tabs">
    <a href="/admin" class="${active.equals("users") ? "active" : ""}">Користувачі та ролі</a>
    <a href="/admin/orgs/sk" class="${active.equals("sk") ? "active" : ""}">Склад СК</a>
    <a href="/admin/orgs/ks" class="${active.equals("ks") ? "active" : ""}">Склад КС</a>
    <a href="/admin/notifications" class="${active.equals("notifications") ? "active" : ""}">Сповіщення</a>
</nav>
