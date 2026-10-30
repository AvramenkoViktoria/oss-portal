@import ua.edu.ukma.oss.web.support.Labels
@import ua.edu.ukma.oss.web.view.VotingView
@param VotingView v
<div class="voting">
    <div class="row between">
        <h3 style="margin:0">${v.voting().question()}</h3>
        <span class="verdict ${v.voting().result().name()}">${Labels.voteResult(v.voting().result())}</span>
    </div>
    <div class="vbar" role="img" aria-label="Підсумок голосування">
        @for(var s : v.segments())
            @if(s.count() > 0)
                <span class="c-${s.choice().name()}" style="width:${s.percent()}%" title="${Labels.choice(s.choice())}: ${s.count()}"></span>
            @endif
        @endfor
    </div>
    <div class="legend">
        @for(var s : v.segments())
            <span><i class="c-${s.choice().name()}"></i>${Labels.choice(s.choice())}: <strong>${s.count()}</strong></span>
        @endfor
        <span class="muted">Присутніх: ${v.voting().present()}</span>
    </div>
    @if(!v.voters().isEmpty())
        <div class="seats" style="grid-template-columns: repeat(${Math.min(10, v.voters().size())}, minmax(0, 1fr)); max-width: ${Math.min(10, v.voters().size()) * 84}px">
            @for(var voter : v.voters())
                <div class="seat c-${voter.choice().name()}" title="${voter.name()} — ${Labels.choice(voter.choice())}">${Labels.initials(voter.name())}</div>
            @endfor
        </div>
        <details>
            <summary class="small muted" style="cursor:pointer">Поіменний список</summary>
            <table class="data">
                <tbody>
                @for(var voter : v.voters())
                    <tr>
                        <td>${voter.name()}</td>
                        <td><span class="legend"><span><i class="c-${voter.choice().name()}"></i>${Labels.choice(voter.choice())}</span></span></td>
                    </tr>
                @endfor
                </tbody>
            </table>
        </details>
    @endif
</div>
