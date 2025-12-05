import React, { useState, useEffect } from 'react';
import { Play, Pause, RotateCcw, SkipForward, Trophy, Zap } from 'lucide-react';
import './App.css';

const App = () => {
  const [gridData, setGridData] = useState(null);
  const [selectedStrategy, setSelectedStrategy] = useState(null);
  const [currentDelivery, setCurrentDelivery] = useState(0);
  const [startDelivery, setstartDelivery] = useState(false);
  const [currentStep, setCurrentStep] = useState(0);
  const [isPlaying, setIsPlaying] = useState(false);
  const [results, setResults] = useState({});
  const [showComparison, setShowComparison] = useState(false);
  const [loading, setLoading] = useState(false);

  const strategies = [
    { code: 'BF', name: 'Breadth-First', color: 'blue' },
    { code: 'DF', name: 'Depth-First', color: 'purple' },
    { code: 'ID', name: 'Iterative Deepening', color: 'indigo' },
    { code: 'UC', name: 'Uniform Cost', color: 'cyan' },
    { code: 'GR1', name: 'Greedy (H1)', color: 'green' },
    { code: 'GR2', name: 'Greedy (H2)', color: 'lime' },
    { code: 'AS1', name: 'A* (H1)', color: 'yellow' },
    { code: 'AS2', name: 'A* (H2)', color: 'orange' }
  ];

  useEffect(() => {
    loadGrid();
  }, []);

  useEffect(() => {
    if (!isPlaying || !selectedStrategy) return;
    const timer = setTimeout(() => {
      nextStep();
    }, 500);
    return () => clearTimeout(timer);
  }, [isPlaying, currentStep, currentDelivery, selectedStrategy]);

  const loadGrid = async () => {
    setLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/grid/generate');
      const data = await response.json();
      setGridData(data);
    } catch (error) {
      console.error('Error loading grid:', error);
    }
    setLoading(false);
  };

  const runStrategy = async (strategyCode) => {
    setLoading(true);
    setSelectedStrategy(strategyCode);
    setCurrentDelivery(0);
    setCurrentStep(0);
    setIsPlaying(false);

    try {
      const response = await fetch('http://localhost:8080/api/solve', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          strategy: strategyCode,
          gridData: gridData
        })
      });
      const result = await response.json();
      //console.log('Result structure:', result);
      //console.log('First delivery:', result.deliveries?.[0]);
      setResults(prev => ({ ...prev, [strategyCode]: result }));
    } catch (error) {
      console.error('Error running strategy:', error);
    }
    setLoading(false);
  };

  const runAllStrategies = async () => {
    setLoading(true);
    try {
      const response = await fetch('http://localhost:8080/api/solve/all', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ gridData: gridData })
      });
      const allResults = await response.json();
      setResults(allResults);
      setShowComparison(true);
    } catch (error) {
      console.error('Error running all strategies:', error);
    }
    setLoading(false);
  };


  const nextStep = () => {
    if (!selectedStrategy || !results[selectedStrategy]) return;
    const currentResult = results[selectedStrategy];
    const delivery = currentResult.deliveries[currentDelivery];
    
    if (!delivery) {
      setIsPlaying(false);
      return;
    }
    //console.log(delivery.truckId);
    if (currentStep < delivery.path.length - 1) {
      setCurrentStep(currentStep + 1);
    } else if (currentDelivery < currentResult.deliveries.length ) {
      setCurrentDelivery(currentDelivery +1);
      setCurrentStep(0);
    } else {
      setIsPlaying(false);
    }
  };
  const reset = () => {
    setCurrentDelivery(0);
    setCurrentStep(0);
    setIsPlaying(false);
  };

  const getOptimalStrategy = () => {
    if (Object.keys(results).length === 0) return null;
    let optimalCost = Infinity;
    let optimal = null;
    Object.entries(results).forEach(([strategy, result]) => {
      if (result.totalCost < optimalCost) {
        optimalCost = result.totalCost;
        optimal = strategy;
      }
    });
    return optimal;
  };

  const getCellContent = (x, y) => {
  if (!gridData) return null;
  //console.log(gridData);
  // Store
  const store = gridData.stores.find(s => s.x === x && s.y === y);
  if (store) {
    const isCurrentStore = selectedStrategy && results[selectedStrategy] &&
      results[selectedStrategy].deliveries[currentDelivery-1]?.truckId === store.id;
          //console.log(`Store ${store.id} at (${x},${y}): isCurrentStore = ${isCurrentStore}`);
    //console.log('Current delivery data:', results[selectedStrategy]?.deliveries[currentDelivery]);
    //console.log('truckId:', results[selectedStrategy]?.deliveries[currentDelivery]?.storeId);
    //console.log('store.id:', store.id);
    
    return { type: 'store', id: store.id, highlight: isCurrentStore };
  }

  // Customer
  const customer = gridData.customers.find(c => c.x === x && c.y === y);
  if (customer) {
    const customerIndex = gridData.customers.findIndex(c => c.id === customer.id);
    const delivered = selectedStrategy && results[selectedStrategy] && currentDelivery > customerIndex;
    return { type: 'customer', id: customer.id, delivered };
  }

  // Tunnel
  const tunnelIndex = gridData.tunnels.findIndex(t => (t.a.x === x && t.a.y === y) || (t.b.x === x && t.b.y === y));
  if (tunnelIndex !== -1) return { type: 'tunnel', id: tunnelIndex };

  return null;
};


 

  useEffect(() => {
  // start when first delivery happens
  if (currentDelivery >= 1 && !startDelivery) {
    setstartDelivery(true);
  }
}, [currentDelivery]);

  
  const getEdgeCost = (x1, y1, x2, y2) => {
    if (!gridData || !gridData.traffic) return 1;
    const key = `${x1},${y1}-${x2},${y2}`;
    return gridData.traffic[key] || 1;
  };

  const optimalStrategy = getOptimalStrategy();

  if (loading) {
    return (
      <div className="loading-screen">
        <div className="loading-text">Loading...</div>
      </div>
    );
  }

  if (!gridData) {
    return (
      <div className="loading-screen">
        <button onClick={loadGrid} className="btn btn-primary btn-large">
          Generate Grid
        </button>
      </div>
    );
  }

  return (
    <div className="app-container">
      <div className="content-wrapper">
        <header className="header">
          <h1>🚚 Delivery Planning Visualizer</h1>
          <p className="subtitle">Compare search strategies for optimal delivery routing</p>
        </header>

        <div className="main-grid">
          <div className="top-section">
            <div className="card">
              <div className="card-header">
                <h2>City Grid</h2>
                <button onClick={loadGrid} className="btn btn-secondary">
                  <RotateCcw size={16} />
                  New Grid
                </button>
              </div>

              <div className="grid-container">
                <div className="grid-wrapper">
                  {Array.from({ length: gridData.m }).map((_, y) => (
                    <div key={y} className="grid-row">
                      {Array.from({ length: gridData.n }).map((_, x) => {
                        const content = getCellContent(x, y);
                        return (
                          <div key={`${x}-${y}`} className="grid-cell">
                            {x < gridData.n - 1 && (
                              <div className="edge-cost edge-right">
                                {getEdgeCost(x, y, x + 1, y)}
                              </div>
                            )}
                            {y < gridData.m - 1 && (
                              <div className="edge-cost edge-bottom">
                                {getEdgeCost(x, y, x, y + 1)}
                              </div>
                            )}

                            <div className="cell-content">
                              {content ? (
                                <div className={`cell-icon ${content.type} ${content.delivered ? 'delivered' : ''} ${content.highlight ? 'highlight' : ''}`}>
                                  {content.type === 'store' && `T${content.id}`}
                                  {content.type === 'customer' && `C${content.id}`}
                                  {content.type === 'tunnel' && `#${content.id}`}
                                </div>
                              ) : (
                                <div className="cell-coords">{x},{y}</div>
                              )}
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  ))}
                </div>
              </div>

              <div className="legend">
                <div className="legend-item">
                  <div className="legend-icon store"></div>
                  <span>Truck/Store</span>
                </div>
                <div className="legend-item">
                  <div className="legend-icon customer"></div>
                  <span>Customer</span>
                </div>
                <div className="legend-item">
                  <div className="legend-icon customer delivered"></div>
                  <span>Delivered</span>
                </div>
                <div className="legend-item">
                  <div className="legend-icon tunnel"></div>
                  <span>Tunnel</span>
                </div>
              </div>
            </div>

            {selectedStrategy && results[selectedStrategy] && (
              <div className="card">
                <h2>Controls</h2>
                <div className="controls-panel">
                  <div className="controls-header">
                    <div>
                      <div className="control-info">Delivery {currentDelivery } / {results[selectedStrategy].deliveries.length}</div>
                      <div className="control-info">Step {currentStep + 1} / {results[selectedStrategy].deliveries[currentDelivery]?.path.length}</div>
                    </div>
                    <div className="cost-display">
                      Cost: {results[selectedStrategy].deliveries[currentDelivery-1]?.cost}
                    </div>
                  </div>

                  <div className="controls-buttons">
                    <button onClick={() => setIsPlaying(!isPlaying)} className="btn btn-primary">
                      {isPlaying ? <Pause size={16} /> : <Play size={16} />}
                      {isPlaying ? 'Pause' : 'Play'}
                    </button>
                    <button onClick={nextStep} className="btn btn-secondary">
                      <SkipForward size={16} />
                      Next
                    </button>
                    <button onClick={reset} className="btn btn-secondary">
                      <RotateCcw size={16} />
                      Reset
                    </button>
                  </div>

                  <div className="path-actions">
                    <div className="path-label">Path:</div>
                    <div className="actions-list">
                      {results[selectedStrategy].deliveries[currentDelivery-1]?.actions?.map((action, idx) => (
                        <span key={idx} className={`action-badge ${idx <= currentStep ? 'active' : ''}`}>
                          {action}
                        </span>
                      ))}
                    </div>
                  </div>
                </div>
              </div>
            )}
          </div>

          <div className="sidebar">
            <div className="card">
              <h2>Search Strategies</h2>
              <div className="strategies-list">
                {strategies.map(strategy => {
                  const isOptimal = optimalStrategy === strategy.code;
                  const hasResult = results[strategy.code];
                  const isSelected = selectedStrategy === strategy.code;
                  
                  return (
                    <button
                      key={strategy.code}
                      onClick={() => runStrategy(strategy.code)}
                      className={`strategy-btn ${isSelected ? 'selected' : ''} ${isOptimal ? 'optimal' : ''} strategy-${strategy.color}`}
                    >
                      <div className="strategy-header">
                        <div>
                          <div className="strategy-code">{strategy.code}</div>
                          <div className="strategy-name">{strategy.name}</div>
                        </div>
                        {isOptimal && <Trophy className="trophy-icon" size={20} />}
                      </div>
                      {hasResult && (
                        <div className="strategy-stats">
                          <div>Cost: {results[strategy.code].totalCost}</div>
                          <div>Nodes: {results[strategy.code].nodesExpanded}</div>
                          <div>Time: {results[strategy.code].timeMs}ms</div>
                        </div>
                      )}
                    </button>
                  );
                })}
              </div>

              <button onClick={runAllStrategies} className="btn btn-gradient">
                <Zap size={20} />
                Run All Strategies
              </button>
            </div>

            {showComparison && Object.keys(results).length > 0 && (
              <div className="card">
                <h2>Comparison</h2>
                <div className="comparison-table">
                  <table>
                    <thead>
                      <tr>
                        <th>Strategy</th>
                        <th>Cost</th>
                        <th>Nodes</th>
                        <th>Time</th>
                      </tr>
                    </thead>
                    <tbody>
                      {Object.entries(results)
                        .sort(([, a], [, b]) => a.totalCost - b.totalCost)
                        .map(([strategy, result]) => (
                          <tr key={strategy} className={strategy === optimalStrategy ? 'optimal-row' : ''}>
                            <td className="strategy-cell">
                              {strategy}
                              {strategy === optimalStrategy && (
                                <Trophy className="trophy-icon-small" size={14} />
                              )}
                            </td>
                            <td>{result.totalCost}</td>
                            <td>{result.nodesExpanded}</td>
                            <td>{result.timeMs}ms</td>
                          </tr>
                        ))}
                    </tbody>
                  </table>
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default App;











